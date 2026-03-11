package com.fromvillage.cart.presentation;

import com.fromvillage.cart.application.CartUpdateCommand;
import jakarta.validation.constraints.NotNull;

public record CartUpdateRequest(
        @NotNull Integer quantity
) {

    public CartUpdateCommand toCommand() {
        return new CartUpdateCommand(quantity);
    }
}
