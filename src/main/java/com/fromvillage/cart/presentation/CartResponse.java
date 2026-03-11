package com.fromvillage.cart.presentation;

import com.fromvillage.cart.application.CartQueryResult;

import java.util.List;

public record CartResponse(
        List<CartItemResponse> items,
        int totalItemCount,
        int totalQuantity,
        long totalAmount
) {

    public static CartResponse from(CartQueryResult result) {
        return new CartResponse(
                result.items().stream()
                        .map(CartItemResponse::from)
                        .toList(),
                result.totalItemCount(),
                result.totalQuantity(),
                result.totalAmount()
        );
    }
}
