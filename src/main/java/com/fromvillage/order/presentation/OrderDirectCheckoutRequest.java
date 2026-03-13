package com.fromvillage.order.presentation;

import com.fromvillage.order.application.OrderDirectCheckoutCommand;
import jakarta.validation.constraints.Min;
import jakarta.validation.constraints.NotNull;

public record OrderDirectCheckoutRequest(
        @NotNull(message = "상품이 선택되지 않았습니다.")
        Long productId,
        @NotNull(message = "주문 수량이 입력되지 않았습니다.")
        @Min(value = 1, message = "주문 수량은 1개 이상이어야 합니다.")
        Integer quantity
) {

    public OrderDirectCheckoutCommand toCommand() {
        return new OrderDirectCheckoutCommand(productId, quantity);
    }
}
