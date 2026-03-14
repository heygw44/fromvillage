package com.fromvillage.order.application;

import com.fromvillage.order.domain.OrderItem;
import com.fromvillage.order.domain.OrderStatus;
import com.fromvillage.order.domain.SellerOrder;

import java.time.LocalDateTime;
import java.util.List;

public record SellerOrderDetail(
        Long sellerOrderId,
        String orderNumber,
        String buyerNickname,
        OrderStatus status,
        Long totalAmount,
        Long discountAmount,
        Long finalAmount,
        LocalDateTime completedAt,
        LocalDateTime canceledAt,
        LocalDateTime createdAt,
        List<OrderItemDetail> orderItems
) {

    public static SellerOrderDetail from(SellerOrder sellerOrder) {
        return new SellerOrderDetail(
                sellerOrder.getId(),
                sellerOrder.getCheckoutOrder().getOrderNumber(),
                sellerOrder.getCheckoutOrder().getUser().getNickname(),
                sellerOrder.getStatus(),
                sellerOrder.getTotalAmount(),
                sellerOrder.getDiscountAmount(),
                sellerOrder.getFinalAmount(),
                sellerOrder.getCompletedAt(),
                sellerOrder.getCanceledAt(),
                sellerOrder.getCreatedAt(),
                sellerOrder.getOrderItems().stream()
                        .map(OrderItemDetail::from)
                        .toList()
        );
    }

    public record OrderItemDetail(
            String productNameSnapshot,
            Long productPriceSnapshot,
            Integer quantity,
            Long lineAmount
    ) {
        public static OrderItemDetail from(OrderItem orderItem) {
            return new OrderItemDetail(
                    orderItem.getProductNameSnapshot(),
                    orderItem.getProductPriceSnapshot(),
                    orderItem.getQuantity(),
                    orderItem.getLineAmount()
            );
        }
    }
}
