package com.fromvillage.coupon.application;

import com.fromvillage.coupon.domain.IssuedCouponStatus;
import com.fromvillage.coupon.domain.IssuedCouponStore;
import lombok.RequiredArgsConstructor;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
@RequiredArgsConstructor
public class CouponQueryService {

    private final IssuedCouponStore issuedCouponStore;

    @PreAuthorize("hasRole('USER')")
    @Transactional(readOnly = true)
    public CouponQueryResult getMyCoupons(Long userId) {
        return CouponQueryResult.from(
                issuedCouponStore.findAllByUserIdAndStatusOrderByIssuedAtDescIdDesc(
                        userId,
                        IssuedCouponStatus.ISSUED
                )
        );
    }
}
