package com.fromvillage.coupon.infrastructure;

import com.fromvillage.coupon.domain.CouponPolicy;
import jakarta.persistence.LockModeType;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Lock;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import java.util.Optional;

public interface CouponPolicyJpaRepository extends JpaRepository<CouponPolicy, Long> {

    @Lock(LockModeType.PESSIMISTIC_WRITE)
    @Query("select couponPolicy from CouponPolicy couponPolicy where couponPolicy.id = :couponPolicyId")
    Optional<CouponPolicy> findByIdForUpdate(@Param("couponPolicyId") Long couponPolicyId);
}
