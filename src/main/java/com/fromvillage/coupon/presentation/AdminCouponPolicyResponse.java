package com.fromvillage.coupon.presentation;

import com.fromvillage.coupon.application.AdminCouponPolicyResult;

import java.time.LocalDateTime;

public record AdminCouponPolicyResponse(
        Long couponPolicyId,
        String name,
        Long discountAmount,
        Long minimumOrderAmount,
        Integer totalQuantity,
        Integer issuedQuantity,
        LocalDateTime startedAt,
        LocalDateTime endedAt,
        String status
) {

    public static AdminCouponPolicyResponse from(AdminCouponPolicyResult result) {
        return new AdminCouponPolicyResponse(
                result.couponPolicyId(),
                result.name(),
                result.discountAmount(),
                result.minimumOrderAmount(),
                result.totalQuantity(),
                result.issuedQuantity(),
                result.startedAt(),
                result.endedAt(),
                result.status().name()
        );
    }
}
