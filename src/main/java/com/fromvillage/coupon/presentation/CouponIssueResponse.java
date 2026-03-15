package com.fromvillage.coupon.presentation;

import com.fromvillage.coupon.application.CouponIssueResult;

import java.time.LocalDateTime;

public record CouponIssueResponse(
        Long issuedCouponId,
        Long couponPolicyId,
        String couponName,
        Long discountAmount,
        Long minimumOrderAmount,
        String status,
        LocalDateTime issuedAt,
        LocalDateTime startedAt,
        LocalDateTime endedAt
) {

    public static CouponIssueResponse from(CouponIssueResult result) {
        return new CouponIssueResponse(
                result.issuedCouponId(),
                result.couponPolicyId(),
                result.couponName(),
                result.discountAmount(),
                result.minimumOrderAmount(),
                result.status().name(),
                result.issuedAt(),
                result.startedAt(),
                result.endedAt()
        );
    }
}
