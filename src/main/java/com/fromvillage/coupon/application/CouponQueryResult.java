package com.fromvillage.coupon.application;

import com.fromvillage.coupon.domain.IssuedCoupon;
import com.fromvillage.coupon.domain.IssuedCouponStatus;

import java.time.LocalDateTime;
import java.util.List;

public record CouponQueryResult(
        List<CouponSummary> coupons
) {

    public static CouponQueryResult from(List<IssuedCoupon> issuedCoupons) {
        return new CouponQueryResult(
                issuedCoupons.stream()
                        .map(CouponSummary::from)
                        .toList()
        );
    }

    public record CouponSummary(
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

        public static CouponSummary from(IssuedCoupon issuedCoupon) {
            return new CouponSummary(
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
}
