package com.fromvillage.order.domain;

import java.time.LocalDateTime;

public record SellerOrderSummaryView(
        Long sellerOrderId,
        String orderNumber,
        String buyerNickname,
        OrderStatus status,
        Long totalAmount,
        Long discountAmount,
        Long finalAmount,
        LocalDateTime completedAt,
        LocalDateTime canceledAt,
        LocalDateTime createdAt
) {
}
