package com.fromvillage.coupon.application;

import com.fromvillage.common.exception.BusinessException;
import com.fromvillage.common.exception.ErrorCode;
import com.fromvillage.coupon.domain.CouponPolicy;
import com.fromvillage.coupon.domain.CouponPolicyStore;
import com.fromvillage.user.domain.User;
import com.fromvillage.user.domain.UserStore;
import lombok.RequiredArgsConstructor;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
@RequiredArgsConstructor
public class AdminCouponPolicyService {

    private final CouponPolicyStore couponPolicyStore;
    private final UserStore userStore;

    @PreAuthorize("hasRole('ADMIN')")
    @Transactional
    public AdminCouponPolicyResult createCouponPolicy(Long adminUserId, AdminCouponPolicyCreateCommand command) {
        User admin = userStore.findById(adminUserId)
                .orElseThrow(() -> new BusinessException(ErrorCode.USER_NOT_FOUND));

        CouponPolicy couponPolicy = CouponPolicy.create(
                command.name(),
                command.discountAmount(),
                command.minimumOrderAmount(),
                command.totalQuantity(),
                command.startedAt(),
                command.endedAt(),
                admin
        );

        CouponPolicy savedCouponPolicy = couponPolicyStore.save(couponPolicy);
        return AdminCouponPolicyResult.from(savedCouponPolicy);
    }

    @PreAuthorize("hasRole('ADMIN')")
    @Transactional
    public AdminCouponPolicyResult openCouponPolicy(Long couponPolicyId) {
        CouponPolicy couponPolicy = couponPolicyStore.findByIdForUpdate(couponPolicyId)
                .orElseThrow(() -> new BusinessException(ErrorCode.COUPON_POLICY_NOT_FOUND));

        couponPolicy.open();
        return AdminCouponPolicyResult.from(couponPolicy);
    }

    @PreAuthorize("hasRole('ADMIN')")
    @Transactional
    public AdminCouponPolicyResult closeCouponPolicy(Long couponPolicyId) {
        CouponPolicy couponPolicy = couponPolicyStore.findByIdForUpdate(couponPolicyId)
                .orElseThrow(() -> new BusinessException(ErrorCode.COUPON_POLICY_NOT_FOUND));

        couponPolicy.close();
        return AdminCouponPolicyResult.from(couponPolicy);
    }
}
