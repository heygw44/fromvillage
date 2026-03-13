package com.fromvillage.order.application;

import com.fromvillage.order.domain.CheckoutOrder;
import com.fromvillage.order.domain.OrderItem;
import com.fromvillage.order.domain.OrderStatus;
import com.fromvillage.order.domain.SellerOrder;

import java.time.LocalDateTime;
import java.util.List;

public record OrderDetail(
        String orderNumber,
        OrderStatus status,
        Long totalAmount,
        Long discountAmount,

        Long finalAmount,
        LocalDateTime completedAt,
        LocalDateTime canceledAt,
        LocalDateTime createdAt,
        List<SellerOrderDetail> sellerOrders
) {

    public static OrderDetail from(CheckoutOrder checkoutOrder) {
        return new OrderDetail(
                checkoutOrder.getOrderNumber(),
                checkoutOrder.getStatus(),
                checkoutOrder.getTotalAmount(),
                checkoutOrder.getDiscountAmount(),
                checkoutOrder.getFinalAmount(),
                checkoutOrder.getCompletedAt(),
                checkoutOrder.getCanceledAt(),
                checkoutOrder.getCreatedAt(),
                checkoutOrder.getSellerOrders().stream()
                        .map(SellerOrderDetail::from)
                        .toList()
        );
    }

    public record SellerOrderDetail(
            String sellerNickname,
            OrderStatus status,
            Long totalAmount,
            Long discountAmount,
            Long finalAmount,
            LocalDateTime completedAt,
            LocalDateTime canceledAt,
            List<OrderItemDetail> orderItems
    ) {
        public static SellerOrderDetail from(SellerOrder sellerOrder) {
            return new SellerOrderDetail(
                    sellerOrder.getSeller().getNickname(),
                    sellerOrder.getStatus(),
                    sellerOrder.getTotalAmount(),
                    sellerOrder.getDiscountAmount(),
                    sellerOrder.getFinalAmount(),
                    sellerOrder.getCompletedAt(),
                    sellerOrder.getCanceledAt(),
                    sellerOrder.getOrderItems().stream()
                            .map(OrderItemDetail::from)
                            .toList()
            );
        }
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
