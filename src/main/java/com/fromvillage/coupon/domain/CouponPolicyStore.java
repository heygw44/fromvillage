package com.fromvillage.coupon.domain;

import java.util.Optional;

public interface CouponPolicyStore {

    CouponPolicy save(CouponPolicy couponPolicy);

    Optional<CouponPolicy> findById(Long couponPolicyId);
}
