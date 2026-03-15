package com.fromvillage.coupon.domain;

import java.util.List;
import java.util.Optional;

public interface IssuedCouponStore {

    IssuedCoupon save(IssuedCoupon issuedCoupon);

    boolean existsByCouponPolicyIdAndUserId(Long couponPolicyId, Long userId);

    List<IssuedCoupon> findAllByUserIdAndStatusOrderByIssuedAtDescIdDesc(Long userId, IssuedCouponStatus status);

    Optional<IssuedCoupon> findById(Long issuedCouponId);
}
