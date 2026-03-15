package com.fromvillage.coupon.infrastructure;

import com.fromvillage.coupon.domain.IssuedCoupon;
import com.fromvillage.coupon.domain.IssuedCouponStatus;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;

import java.util.List;

public interface IssuedCouponJpaRepository extends JpaRepository<IssuedCoupon, Long> {

    boolean existsByCouponPolicyIdAndUserId(Long couponPolicyId, Long userId);

    @Query("""
            select issuedCoupon
            from IssuedCoupon issuedCoupon
            join fetch issuedCoupon.couponPolicy couponPolicy
            where issuedCoupon.user.id = :userId
              and issuedCoupon.status = :status
            order by issuedCoupon.issuedAt desc, issuedCoupon.id desc
            """)
    List<IssuedCoupon> findAllByUserIdAndStatusOrderByIssuedAtDescIdDesc(Long userId, IssuedCouponStatus status);
}
