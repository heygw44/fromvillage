package com.fromvillage.coupon.application;

import java.time.LocalDateTime;

public record AdminCouponPolicyCreateCommand(
        String name,
        Long discountAmount,
        Long minimumOrderAmount,
        Integer totalQuantity,
        LocalDateTime startedAt,
        LocalDateTime endedAt
) {
}
