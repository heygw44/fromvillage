package com.fromvillage.order.application;

import com.fromvillage.common.exception.BusinessException;
import com.fromvillage.common.exception.ErrorCode;
import com.fromvillage.order.domain.CheckoutOrder;
import com.fromvillage.order.domain.CheckoutOrderStore;
import com.fromvillage.order.domain.OrderItem;
import com.fromvillage.order.domain.SellerOrder;
import com.fromvillage.product.domain.Product;
import com.fromvillage.product.domain.ProductStatus;
import com.fromvillage.user.domain.User;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;

import java.time.Clock;
import java.time.LocalDateTime;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.stream.Collectors;

@Service
@RequiredArgsConstructor
public class OrderPlacementService {

    private final CheckoutOrderStore checkoutOrderStore;
    private final Clock clock;

    public CheckoutOrder place(User user, List<OrderCheckoutLine> orderLines) {
        User orderUser = Objects.requireNonNull(user);
        List<OrderCheckoutLine> lines = requireOrderLines(orderLines);

        validateOrderLines(lines);
        decreaseStocks(lines);

        CheckoutOrder checkoutOrder = createCheckoutOrder(orderUser, lines);
        checkoutOrder.complete(LocalDateTime.now(clock));
        return checkoutOrderStore.save(checkoutOrder);
    }

    private List<OrderCheckoutLine> requireOrderLines(List<OrderCheckoutLine> orderLines) {
        List<OrderCheckoutLine> lines = Objects.requireNonNull(orderLines);
        if (lines.isEmpty()) {
            throw new BusinessException(ErrorCode.ORDER_ITEMS_REQUIRED);
        }
        return lines;
    }

    private void validateOrderLines(List<OrderCheckoutLine> orderLines) {
        for (OrderCheckoutLine orderLine : orderLines) {
            Product product = orderLine.product();
            if (product.isDeleted() || product.getStatus() != ProductStatus.ON_SALE) {
                throw new BusinessException(ErrorCode.ORDER_PRODUCT_UNAVAILABLE);
            }
        }
    }

    private void decreaseStocks(List<OrderCheckoutLine> orderLines) {
        for (OrderCheckoutLine orderLine : orderLines) {
            orderLine.product().decreaseStock(orderLine.quantity());
        }
    }

    private CheckoutOrder createCheckoutOrder(User user, List<OrderCheckoutLine> orderLines) {
        Map<Long, List<OrderCheckoutLine>> orderLinesBySellerId = orderLines.stream()
                .collect(Collectors.groupingBy(
                        orderLine -> orderLine.product().getSeller().getId(),
                        LinkedHashMap::new,
                        Collectors.toList()
                ));

        List<SellerOrder> sellerOrders = orderLinesBySellerId.values().stream()
                .map(this::createSellerOrder)
                .toList();

        return CheckoutOrder.create(user, sellerOrders);
    }

    private SellerOrder createSellerOrder(List<OrderCheckoutLine> orderLines) {
        User seller = orderLines.get(0).product().getSeller();
        List<OrderItem> orderItems = orderLines.stream()
                .map(orderLine -> OrderItem.create(orderLine.product(), orderLine.quantity()))
                .toList();

        return SellerOrder.create(seller, orderItems);
    }
}
