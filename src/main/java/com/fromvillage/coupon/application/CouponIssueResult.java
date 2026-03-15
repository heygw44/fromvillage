package com.fromvillage.coupon.application;

import com.fromvillage.coupon.domain.IssuedCoupon;
import com.fromvillage.coupon.domain.IssuedCouponStatus;

import java.time.LocalDateTime;

public record CouponIssueResult(
        Long issuedCouponId,
        Long couponPolicyId,
        String couponName,
        Long discountAmount,
        Long minimumOrderAmount,
        IssuedCouponStatus status,
        LocalDateTime issuedAt,
        LocalDateTime startedAt,
        LocalDateTime endedAt
) {

    public static CouponIssueResult from(IssuedCoupon issuedCoupon) {
        return new CouponIssueResult(
                issuedCoupon.getId(),
                issuedCoupon.getCouponPolicy().getId(),
                issuedCoupon.getCouponPolicy().getName(),
                issuedCoupon.getCouponPolicy().getDiscountAmount(),
                issuedCoupon.getCouponPolicy().getMinimumOrderAmount(),
                issuedCoupon.getStatus(),
                issuedCoupon.getIssuedAt(),
                issuedCoupon.getCouponPolicy().getStartedAt(),
                issuedCoupon.getCouponPolicy().getEndedAt()
        );
    }
}
