package com.fromvillage.order.application;

import com.fromvillage.order.domain.CheckoutOrder;
import com.fromvillage.order.domain.CheckoutOrderSummaryView;
import com.fromvillage.order.domain.OrderStatus;

import java.time.LocalDateTime;

public record OrderSummary(
        Long orderId,
        OrderStatus status,
        Integer sellerOrderCount,
        Long totalAmount,
        Long discountAmount,
        Long finalAmount,
        LocalDateTime completedAt,
        LocalDateTime canceledAt,
        LocalDateTime createdAt
) {

    public static OrderSummary from(CheckoutOrder checkoutOrder) {
        return new OrderSummary(
                checkoutOrder.getId(),
                checkoutOrder.getStatus(),
                checkoutOrder.getSellerOrders().size(),
                checkoutOrder.getTotalAmount(),
                checkoutOrder.getDiscountAmount(),
                checkoutOrder.getFinalAmount(),
                checkoutOrder.getCompletedAt(),
                checkoutOrder.getCanceledAt(),
                checkoutOrder.getCreatedAt()
        );
    }

    public static OrderSummary from(CheckoutOrderSummaryView summaryView) {
        return new OrderSummary(
                summaryView.orderId(),
                summaryView.status(),
                Math.toIntExact(summaryView.sellerOrderCount()),
                summaryView.totalAmount(),
                summaryView.discountAmount(),
                summaryView.finalAmount(),
                summaryView.completedAt(),
                summaryView.canceledAt(),
                summaryView.createdAt()
        );
    }
}
