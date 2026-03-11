package com.fromvillage.cart.application;

public record CartCreateCommand(
        Long productId,
        Integer quantity
) {
}
