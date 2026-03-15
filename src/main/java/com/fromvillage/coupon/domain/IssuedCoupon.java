package com.fromvillage.coupon.domain;

import com.fromvillage.common.persistence.BaseTimeEntity;
import com.fromvillage.user.domain.User;
import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.EnumType;
import jakarta.persistence.Enumerated;
import jakarta.persistence.FetchType;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.JoinColumn;
import jakarta.persistence.ManyToOne;
import jakarta.persistence.Table;
import lombok.AccessLevel;
import lombok.Getter;
import lombok.NoArgsConstructor;

import java.time.LocalDateTime;
import java.util.Objects;

@Entity
@Table(name = "issued_coupon")
@Getter
@NoArgsConstructor(access = AccessLevel.PROTECTED)
public class IssuedCoupon extends BaseTimeEntity {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "coupon_policy_id", nullable = false)
    private CouponPolicy couponPolicy;

    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "user_id", nullable = false)
    private User user;

    @Enumerated(EnumType.STRING)
    @Column(nullable = false, length = 20)
    private IssuedCouponStatus status;

    @Column(name = "issued_at", nullable = false)
    private LocalDateTime issuedAt;

    @Column(name = "used_at")
    private LocalDateTime usedAt;

    private IssuedCoupon(
            CouponPolicy couponPolicy,
            User user,
            LocalDateTime issuedAt
    ) {
        this.couponPolicy = Objects.requireNonNull(couponPolicy);
        this.user = Objects.requireNonNull(user);
        this.status = IssuedCouponStatus.ISSUED;
        this.issuedAt = Objects.requireNonNull(issuedAt);
        this.usedAt = null;
    }

    public static IssuedCoupon issue(
            CouponPolicy couponPolicy,
            User user,
            LocalDateTime issuedAt
    ) {
        return new IssuedCoupon(couponPolicy, user, issuedAt);
    }

    public void use(LocalDateTime usedAt) {
        this.status = IssuedCouponStatus.USED;
        this.usedAt = Objects.requireNonNull(usedAt);
    }

    public void restore() {
        this.status = IssuedCouponStatus.ISSUED;
        this.usedAt = null;
    }
}
