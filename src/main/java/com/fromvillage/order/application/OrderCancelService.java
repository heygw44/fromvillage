package com.fromvillage.order.application;

import com.fromvillage.common.exception.BusinessException;
import com.fromvillage.common.exception.ErrorCode;
import com.fromvillage.order.domain.CheckoutOrder;
import com.fromvillage.order.domain.CheckoutOrderQueryPort;
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

    private final CheckoutOrderQueryPort checkoutOrderQueryPort;
    private final Clock clock;

    @PreAuthorize("hasRole('USER')")
    @Transactional
    public OrderSummary cancel(Long userId, Long orderId) {
        validateOwnership(userId, orderId);
        CheckoutOrder checkoutOrder = checkoutOrderQueryPort.findDetailById(orderId)
                .orElseThrow(() -> new BusinessException(ErrorCode.ORDER_NOT_FOUND));

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

    private void validateOwnership(Long userId, Long orderId) {
        Long ownerId = checkoutOrderQueryPort.findOwnerIdById(orderId)
                .orElseThrow(() -> new BusinessException(ErrorCode.ORDER_NOT_FOUND));

        if (!Objects.equals(ownerId, userId)) {
            throw new BusinessException(ErrorCode.AUTH_FORBIDDEN);
        }
    }
}
