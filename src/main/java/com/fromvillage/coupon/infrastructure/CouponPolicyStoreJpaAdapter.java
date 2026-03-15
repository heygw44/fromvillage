package com.fromvillage.coupon.infrastructure;

import com.fromvillage.coupon.domain.CouponPolicy;
import com.fromvillage.coupon.domain.CouponPolicyStore;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Repository;

import java.util.Optional;

@Repository
@RequiredArgsConstructor
public class CouponPolicyStoreJpaAdapter implements CouponPolicyStore {

    private final CouponPolicyJpaRepository couponPolicyJpaRepository;

    @Override
    public CouponPolicy save(CouponPolicy couponPolicy) {
        return couponPolicyJpaRepository.save(couponPolicy);
    }

    @Override
    public Optional<CouponPolicy> findById(Long couponPolicyId) {
        return couponPolicyJpaRepository.findById(couponPolicyId);
    }

    @Override
    public Optional<CouponPolicy> findByIdForUpdate(Long couponPolicyId) {
        return couponPolicyJpaRepository.findByIdForUpdate(couponPolicyId);
    }
}
