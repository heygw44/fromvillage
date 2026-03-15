package com.fromvillage.coupon.presentation;

import com.fromvillage.coupon.application.AdminCouponPolicyCreateCommand;
import jakarta.validation.constraints.AssertTrue;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Positive;
import jakarta.validation.constraints.PositiveOrZero;
import jakarta.validation.constraints.Size;

import java.time.LocalDateTime;

public record AdminCouponPolicyCreateRequest(
        @NotBlank(message = "쿠폰명이 입력되지 않았습니다.")
        @Size(max = 255, message = "쿠폰명은 255자 이하로 입력해 주세요.")
        String name,

        @NotNull(message = "할인 금액이 입력되지 않았습니다.")
        @Positive(message = "할인 금액은 1원 이상이어야 합니다.")
        Long discountAmount,

        @NotNull(message = "최소 주문 금액이 입력되지 않았습니다.")
        @PositiveOrZero(message = "최소 주문 금액은 0원 이상이어야 합니다.")
        Long minimumOrderAmount,

        @NotNull(message = "총 발급 수량이 입력되지 않았습니다.")
        @Positive(message = "총 발급 수량은 1개 이상이어야 합니다.")
        Integer totalQuantity,

        @NotNull(message = "발급 시작 시각이 입력되지 않았습니다.")
        LocalDateTime startedAt,

        @NotNull(message = "발급 종료 시각이 입력되지 않았습니다.")
        LocalDateTime endedAt
) {

    public AdminCouponPolicyCreateCommand toCommand() {
        return new AdminCouponPolicyCreateCommand(
                name,
                discountAmount,
                minimumOrderAmount,
                totalQuantity,
                startedAt,
                endedAt
        );
    }

    @AssertTrue(message = "쿠폰 발급 기간을 다시 확인해 주세요.")
    public boolean isIssuePeriodValid() {
        if (startedAt == null || endedAt == null) {
            return true;
        }
        return startedAt.isBefore(endedAt);
    }
}
