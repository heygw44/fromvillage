package com.fromvillage.cart.presentation;

import com.fromvillage.cart.application.CartCreateCommand;
import jakarta.validation.constraints.Min;
import jakarta.validation.constraints.NotNull;

public record CartCreateRequest(
        @NotNull(message = "상품이 선택되지 않았습니다.")
        Long productId,
        @NotNull(message = "장바구니 수량이 입력되지 않았습니다.")
        @Min(value = 1, message = "장바구니 수량은 1개 이상이어야 합니다.")
        Integer quantity
) {

    public CartCreateCommand toCommand() {
        return new CartCreateCommand(productId, quantity);
    }
}
