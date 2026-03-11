package com.fromvillage.cart.presentation;

import com.fromvillage.cart.application.CartCreateCommand;
import jakarta.validation.constraints.NotNull;

public record CartCreateRequest(
        @NotNull Long productId,
        @NotNull Integer quantity
) {

    public CartCreateCommand toCommand() {
        return new CartCreateCommand(productId, quantity);
    }
}
