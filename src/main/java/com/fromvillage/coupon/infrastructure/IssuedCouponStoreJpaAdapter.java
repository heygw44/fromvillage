package com.fromvillage.coupon.infrastructure;

import com.fromvillage.coupon.domain.IssuedCoupon;
import com.fromvillage.coupon.domain.IssuedCouponStatus;
import com.fromvillage.coupon.domain.IssuedCouponStore;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.Optional;

@Repository
@RequiredArgsConstructor
public class IssuedCouponStoreJpaAdapter implements IssuedCouponStore {

    private final IssuedCouponJpaRepository issuedCouponJpaRepository;

    @Override
    public IssuedCoupon save(IssuedCoupon issuedCoupon) {
        return issuedCouponJpaRepository.save(issuedCoupon);
    }

    @Override
    public boolean existsByCouponPolicyIdAndUserId(Long couponPolicyId, Long userId) {
        return issuedCouponJpaRepository.existsByCouponPolicyIdAndUserId(couponPolicyId, userId);
    }

    @Override
    public List<IssuedCoupon> findAllByUserIdAndStatusOrderByIssuedAtDescIdDesc(Long userId, IssuedCouponStatus status) {
        return issuedCouponJpaRepository.findAllByUserIdAndStatusOrderByIssuedAtDescIdDesc(userId, status);
    }

    @Override
    public Optional<IssuedCoupon> findById(Long issuedCouponId) {
        return issuedCouponJpaRepository.findById(issuedCouponId);
    }
}
