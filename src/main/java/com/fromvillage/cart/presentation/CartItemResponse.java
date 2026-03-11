package com.fromvillage.cart.presentation;

import com.fromvillage.cart.application.CartItemSummary;

public record CartItemResponse(
        Long cartItemId,
        Long productId,
        Long sellerId,
        String productName,
        String imageUrl,
        Long price,
        Integer quantity,
        Long lineAmount
) {

    public static CartItemResponse from(CartItemSummary summary) {
        return new CartItemResponse(
                summary.cartItemId(),
                summary.productId(),
                summary.sellerId(),
                summary.productName(),
                summary.imageUrl(),
                summary.price(),
                summary.quantity(),
                summary.lineAmount()
        );
    }
}
