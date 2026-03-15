package com.fromvillage.coupon.infrastructure;

import com.fromvillage.coupon.domain.CouponPolicy;
import com.fromvillage.coupon.domain.CouponPolicyStore;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Component;

import java.util.Optional;

@Component
@RequiredArgsConstructor
public class CouponPolicyStoreJpaAdapter implements CouponPolicyStore {

    private final CouponPolicyJpaRepository couponPolicyJpaRepository;

    @Override
    public CouponPolicy save(CouponPolicy couponPolicy) {
        return couponPolicyJpaRepository.saveAndFlush(couponPolicy);
    }

    @Override
    public Optional<CouponPolicy> findById(Long couponPolicyId) {
        return couponPolicyJpaRepository.findById(couponPolicyId);
    }
}
