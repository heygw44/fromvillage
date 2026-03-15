package com.fromvillage.coupon.application;

import com.fromvillage.coupon.domain.CouponPolicy;
import com.fromvillage.coupon.domain.CouponPolicyStatus;

import java.time.LocalDateTime;

public record AdminCouponPolicyResult(
        Long couponPolicyId,
        String name,
        Long discountAmount,
        Long minimumOrderAmount,
        Integer totalQuantity,
        Integer issuedQuantity,
        LocalDateTime startedAt,
        LocalDateTime endedAt,
        CouponPolicyStatus status
) {

    public static AdminCouponPolicyResult from(CouponPolicy couponPolicy) {
        return new AdminCouponPolicyResult(
                couponPolicy.getId(),
                couponPolicy.getName(),
                couponPolicy.getDiscountAmount(),
                couponPolicy.getMinimumOrderAmount(),
                couponPolicy.getTotalQuantity(),
                couponPolicy.getIssuedQuantity(),
                couponPolicy.getStartedAt(),
                couponPolicy.getEndedAt(),
                couponPolicy.getStatus()
        );
    }
}
