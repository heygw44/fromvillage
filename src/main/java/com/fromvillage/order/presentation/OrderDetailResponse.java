package com.fromvillage.order.presentation;

import com.fromvillage.order.application.OrderDetail;

import java.time.LocalDateTime;
import java.util.List;

public record OrderDetailResponse(
        String orderNumber,
        String status,
        Long totalAmount,
        Long discountAmount,
        Long finalAmount,
        LocalDateTime completedAt,
        LocalDateTime canceledAt,
        LocalDateTime createdAt,
        List<SellerOrderDetailResponse> sellerOrders
) {

    public static OrderDetailResponse from(OrderDetail detail) {
        return new OrderDetailResponse(
                detail.orderNumber(),
                detail.status().name(),
                detail.totalAmount(),
                detail.discountAmount(),
                detail.finalAmount(),
                detail.completedAt(),
                detail.canceledAt(),
                detail.createdAt(),
                detail.sellerOrders().stream()
                        .map(SellerOrderDetailResponse::from)
                        .toList()
        );
    }

    public record SellerOrderDetailResponse(
            String sellerNickname,
            String status,
            Long totalAmount,
            Long discountAmount,
            Long finalAmount,
            LocalDateTime completedAt,
            LocalDateTime canceledAt,
            List<OrderItemDetailResponse> orderItems
    ) {
        public static SellerOrderDetailResponse from(OrderDetail.SellerOrderDetail detail) {
            return new SellerOrderDetailResponse(
                    detail.sellerNickname(),
                    detail.status().name(),
                    detail.totalAmount(),
                    detail.discountAmount(),
                    detail.finalAmount(),
                    detail.completedAt(),
                    detail.canceledAt(),
                    detail.orderItems().stream()
                            .map(OrderItemDetailResponse::from)
                            .toList()
            );
        }
    }

    public record OrderItemDetailResponse(
            String productNameSnapshot,
            Long productPriceSnapshot,
            Integer quantity,
            Long lineAmount
    ) {
        public static OrderItemDetailResponse from(OrderDetail.OrderItemDetail detail) {
            return new OrderItemDetailResponse(
                    detail.productNameSnapshot(),
                    detail.productPriceSnapshot(),
                    detail.quantity(),
                    detail.lineAmount()
            );
        }
    }
}
