package com.fromvillage.coupon.application;

import com.fromvillage.common.exception.BusinessException;
import com.fromvillage.common.exception.ErrorCode;
import com.fromvillage.coupon.domain.CouponPolicy;
import com.fromvillage.coupon.domain.CouponPolicyStore;
import com.fromvillage.coupon.domain.IssuedCoupon;
import com.fromvillage.coupon.domain.IssuedCouponStore;
import com.fromvillage.user.domain.User;
import com.fromvillage.user.domain.UserStore;
import lombok.RequiredArgsConstructor;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.Clock;
import java.time.LocalDateTime;

@Service
@RequiredArgsConstructor
public class CouponIssueService {

    private final UserStore userStore;
    private final CouponPolicyStore couponPolicyStore;
    private final IssuedCouponStore issuedCouponStore;
    private final Clock clock;

    @PreAuthorize("hasRole('USER')")
    @Transactional
    public CouponIssueResult issue(Long userId, Long couponPolicyId) {
        User user = userStore.findById(userId)
                .orElseThrow(() -> new BusinessException(ErrorCode.USER_NOT_FOUND));

        CouponPolicy couponPolicy = couponPolicyStore.findById(couponPolicyId)
                .orElseThrow(() -> new BusinessException(ErrorCode.COUPON_POLICY_NOT_FOUND));

        if (issuedCouponStore.existsByCouponPolicyIdAndUserId(couponPolicyId, userId)) {
            throw new BusinessException(ErrorCode.COUPON_ALREADY_ISSUED);
        }

        LocalDateTime issuedAt = LocalDateTime.now(clock);
        couponPolicy.issue(issuedAt);

        IssuedCoupon issuedCoupon = IssuedCoupon.issue(couponPolicy, user, issuedAt);
        IssuedCoupon savedIssuedCoupon = issuedCouponStore.save(issuedCoupon);

        return CouponIssueResult.from(savedIssuedCoupon);
    }
}
