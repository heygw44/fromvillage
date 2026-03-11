package com.fromvillage.cart.presentation;

import com.fromvillage.cart.application.CartUpdateCommand;
import jakarta.validation.constraints.Min;
import jakarta.validation.constraints.NotNull;

public record CartUpdateRequest(
        @NotNull(message = "장바구니 수량이 입력되지 않았습니다.")
        @Min(value = 1, message = "장바구니 수량은 1개 이상이어야 합니다.")
        Integer quantity
) {

    public CartUpdateCommand toCommand() {
        return new CartUpdateCommand(quantity);
    }
}
