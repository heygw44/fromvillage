package com.fromvillage.order.presentation;

import com.fromvillage.order.application.SellerOrderDetail;

import java.time.LocalDateTime;
import java.util.List;

public record SellerOrderDetailResponse(
        Long sellerOrderId,
        String orderNumber,
        String buyerNickname,
        String status,
        Long totalAmount,
        Long discountAmount,
        Long finalAmount,
        LocalDateTime completedAt,
        LocalDateTime canceledAt,
        LocalDateTime createdAt,
        List<OrderItemDetailResponse> orderItems
) {

    public static SellerOrderDetailResponse from(SellerOrderDetail detail) {
        return new SellerOrderDetailResponse(
                detail.sellerOrderId(),
                detail.orderNumber(),
                detail.buyerNickname(),
                detail.status().name(),
                detail.totalAmount(),
                detail.discountAmount(),
                detail.finalAmount(),
                detail.completedAt(),
                detail.canceledAt(),
                detail.createdAt(),
                detail.orderItems().stream()
                        .map(OrderItemDetailResponse::from)
                        .toList()
        );
    }

    public record OrderItemDetailResponse(
            String productNameSnapshot,
            Long productPriceSnapshot,
            Integer quantity,
            Long lineAmount
    ) {
        public static OrderItemDetailResponse from(SellerOrderDetail.OrderItemDetail detail) {
            return new OrderItemDetailResponse(
                    detail.productNameSnapshot(),
                    detail.productPriceSnapshot(),
                    detail.quantity(),
                    detail.lineAmount()
            );
        }
    }
}
