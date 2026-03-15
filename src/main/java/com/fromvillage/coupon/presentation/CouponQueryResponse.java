package com.fromvillage.coupon.presentation;

import com.fromvillage.coupon.application.CouponQueryResult;

import java.time.LocalDateTime;
import java.util.List;

public record CouponQueryResponse(
        List<CouponSummaryResponse> coupons
) {

    public static CouponQueryResponse from(CouponQueryResult result) {
        return new CouponQueryResponse(
                result.coupons().stream()
                        .map(CouponSummaryResponse::from)
                        .toList()
        );
    }

    public record CouponSummaryResponse(
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

        public static CouponSummaryResponse from(CouponQueryResult.CouponSummary summary) {
            return new CouponSummaryResponse(
                    summary.issuedCouponId(),
                    summary.couponPolicyId(),
                    summary.couponName(),
                    summary.discountAmount(),
                    summary.minimumOrderAmount(),
                    summary.status().name(),
                    summary.issuedAt(),
                    summary.startedAt(),
                    summary.endedAt()
            );
        }
    }
}
