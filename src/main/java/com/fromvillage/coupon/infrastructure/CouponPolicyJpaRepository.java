package com.fromvillage.coupon.infrastructure;

import com.fromvillage.coupon.domain.CouponPolicy;
import org.springframework.data.jpa.repository.JpaRepository;

public interface CouponPolicyJpaRepository extends JpaRepository<CouponPolicy, Long> {
}
