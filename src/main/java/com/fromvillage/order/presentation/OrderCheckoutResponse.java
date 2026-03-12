package com.fromvillage.order.presentation;

import com.fromvillage.order.application.OrderCheckoutResult;

import java.time.LocalDateTime;

public record OrderCheckoutResponse(
        Long orderId,
        String status,
        Integer sellerOrderCount,
        Long totalAmount,
        Long discountAmount,
        Long finalAmount,
        LocalDateTime completedAt
) {

    public static OrderCheckoutResponse from(OrderCheckoutResult result) {
        return new OrderCheckoutResponse(
                result.orderId(),
                result.status().name(),
                result.sellerOrderCount(),
                result.totalAmount(),
                result.discountAmount(),
                result.finalAmount(),
                result.completedAt()
        );
    }
}