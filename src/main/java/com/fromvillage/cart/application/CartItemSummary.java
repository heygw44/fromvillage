package com.fromvillage.cart.application;

import com.fromvillage.cart.domain.CartItem;

public record CartItemSummary(
        Long cartItemId,
        Long productId,
        Long sellerId,
        String productName,
        String imageUrl,
        Long price,
        Integer quantity,
        Long lineAmount
) {

    public static CartItemSummary from(CartItem cartItem) {
        return new CartItemSummary(
                cartItem.getId(),
                cartItem.getProduct().getId(),
                cartItem.getProduct().getSeller().getId(),
                cartItem.getProduct().getName(),
                cartItem.getProduct().getImageUrl(),
                cartItem.getProduct().getPrice(),
                cartItem.getQuantity(),
                cartItem.getProduct().getPrice() * cartItem.getQuantity()
        );
    }
}
