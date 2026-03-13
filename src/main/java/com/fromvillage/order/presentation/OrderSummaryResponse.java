package com.fromvillage.order.presentation;

import com.fromvillage.order.application.OrderSummary;

import java.time.LocalDateTime;

public record OrderSummaryResponse(
        String orderNumber,
        String status,
        Integer sellerOrderCount,
        Long totalAmount,
        Long discountAmount,
        Long finalAmount,
        LocalDateTime completedAt,
        LocalDateTime canceledAt,
        LocalDateTime createdAt
) {

    public static OrderSummaryResponse from(OrderSummary summary) {
        return new OrderSummaryResponse(
                summary.orderNumber(),
                summary.status().name(),
                summary.sellerOrderCount(),
                summary.totalAmount(),
                summary.discountAmount(),
                summary.finalAmount(),
                summary.completedAt(),
                summary.canceledAt(),
                summary.createdAt()
        );
    }
}
