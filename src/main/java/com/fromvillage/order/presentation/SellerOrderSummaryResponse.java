package com.fromvillage.order.presentation;

import com.fromvillage.order.application.SellerOrderSummary;

import java.time.LocalDateTime;

public record SellerOrderSummaryResponse(
        Long sellerOrderId,
        String orderNumber,
        String buyerNickname,
        String status,
        Long totalAmount,
        Long discountAmount,
        Long finalAmount,
        LocalDateTime completedAt,
        LocalDateTime canceledAt,
        LocalDateTime createdAt
) {

    public static SellerOrderSummaryResponse from(SellerOrderSummary summary) {
        return new SellerOrderSummaryResponse(
                summary.sellerOrderId(),
                summary.orderNumber(),
                summary.buyerNickname(),
                summary.status().name(),
                summary.totalAmount(),
                summary.discountAmount(),
                summary.finalAmount(),
                summary.completedAt(),
                summary.canceledAt(),
                summary.createdAt()
        );
    }
}
