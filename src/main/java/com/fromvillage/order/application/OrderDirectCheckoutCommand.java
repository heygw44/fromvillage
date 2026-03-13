package com.fromvillage.order.application;

public record OrderDirectCheckoutCommand(
        Long productId,
        Integer quantity
) {
}
