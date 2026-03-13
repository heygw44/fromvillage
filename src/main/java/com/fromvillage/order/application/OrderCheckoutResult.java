package com.fromvillage.order.application;

import com.fromvillage.order.domain.CheckoutOrder;
import com.fromvillage.order.domain.OrderStatus;

import java.time.LocalDateTime;

public record OrderCheckoutResult(
        String orderNumber,
        OrderStatus status,
        Integer sellerOrderCount,
        Long totalAmount,
        Long discountAmount,
        Long finalAmount,
        LocalDateTime completedAt
) {

    public static OrderCheckoutResult from(CheckoutOrder checkoutOrder) {
        return new OrderCheckoutResult(
                checkoutOrder.getOrderNumber(),
                checkoutOrder.getStatus(),
                checkoutOrder.getSellerOrders().size(),
                checkoutOrder.getTotalAmount(),
                checkoutOrder.getDiscountAmount(),
                checkoutOrder.getFinalAmount(),
                checkoutOrder.getCompletedAt()
        );
    }
}
