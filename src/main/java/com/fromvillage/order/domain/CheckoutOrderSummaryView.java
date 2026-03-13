package com.fromvillage.order.domain;

import java.time.LocalDateTime;

public record CheckoutOrderSummaryView(
        Long orderId,
        OrderStatus status,
        Long sellerOrderCount,
        Long totalAmount,
        Long discountAmount,
        Long finalAmount,
        LocalDateTime completedAt,
        LocalDateTime canceledAt,
        LocalDateTime createdAt
) {
}
