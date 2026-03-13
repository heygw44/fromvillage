package com.fromvillage.order.application;

import com.fromvillage.common.exception.BusinessException;
import com.fromvillage.common.exception.ErrorCode;
import com.fromvillage.order.domain.CheckoutOrder;
import com.fromvillage.order.domain.CheckoutOrderStore;
import com.fromvillage.order.domain.OrderItem;
import lombok.RequiredArgsConstructor;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.Clock;
import java.time.LocalDateTime;
import java.util.Objects;

@Service
@RequiredArgsConstructor
public class OrderCancelService {

    private final CheckoutOrderStore checkoutOrderStore;
    private final Clock clock;

    @PreAuthorize("hasRole('USER')")
    @Transactional
    public OrderSummary cancel(Long userId, Long orderId) {
        CheckoutOrder checkoutOrder = checkoutOrderStore.findById(orderId)
                .orElseThrow(() -> new BusinessException(ErrorCode.ORDER_NOT_FOUND));

        if (!Objects.equals(checkoutOrder.getUser().getId(), userId)) {
            throw new BusinessException(ErrorCode.AUTH_FORBIDDEN);
        }

        checkoutOrder.cancel(LocalDateTime.now(clock));
        restoreStocks(checkoutOrder);

        return OrderSummary.from(checkoutOrder);
    }

    private void restoreStocks(CheckoutOrder checkoutOrder) {
        checkoutOrder.getSellerOrders().stream()
                .flatMap(sellerOrder -> sellerOrder.getOrderItems().stream())
                .forEach(this::restoreStock);
    }

    private void restoreStock(OrderItem orderItem) {
        orderItem.getProduct().restoreStock(orderItem.getQuantity());
    }
}
