package com.fromvillage.order.application;

import com.fromvillage.order.domain.OrderStatus;
import com.fromvillage.order.domain.SellerOrderSummaryView;

import java.time.LocalDateTime;

public record SellerOrderSummary(
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

    public static SellerOrderSummary from(SellerOrderSummaryView view) {
        return new SellerOrderSummary(
                view.sellerOrderId(),
                view.orderNumber(),
                view.buyerNickname(),
                view.status(),
                view.totalAmount(),
                view.discountAmount(),
                view.finalAmount(),
                view.completedAt(),
                view.canceledAt(),
                view.createdAt()
        );
    }
}
