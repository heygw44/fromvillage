package com.fromvillage.coupon.domain;

import com.fromvillage.common.exception.BusinessException;
import com.fromvillage.common.exception.ErrorCode;
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
@Table(name = "coupon_policy")
@Getter
@NoArgsConstructor(access = AccessLevel.PROTECTED)
public class CouponPolicy extends BaseTimeEntity {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(nullable = false, length = 255)
    private String name;

    @Column(name = "discount_amount", nullable = false)
    private Long discountAmount;

    @Column(name = "minimum_order_amount", nullable = false)
    private Long minimumOrderAmount;

    @Column(name = "total_quantity", nullable = false)
    private Integer totalQuantity;

    @Column(name = "issued_quantity", nullable = false)
    private Integer issuedQuantity;

    @Column(name = "started_at", nullable = false)
    private LocalDateTime startedAt;

    @Column(name = "ended_at", nullable = false)
    private LocalDateTime endedAt;

    @Enumerated(EnumType.STRING)
    @Column(nullable = false, length = 20)
    private CouponPolicyStatus status;

    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "created_by", nullable = false)
    private User createdBy;

    private CouponPolicy(
            String name,
            Long discountAmount,
            Long minimumOrderAmount,
            Integer totalQuantity,
            LocalDateTime startedAt,
            LocalDateTime endedAt,
            User createdBy
    ) {
        validateDiscountAmount(discountAmount);
        validateMinimumOrderAmount(minimumOrderAmount);
        validateTotalQuantity(totalQuantity);
        validateIssuePeriod(startedAt, endedAt);

        this.name = Objects.requireNonNull(name);
        this.discountAmount = discountAmount;
        this.minimumOrderAmount = minimumOrderAmount;
        this.totalQuantity = totalQuantity;
        this.issuedQuantity = 0;
        this.startedAt = startedAt;
        this.endedAt = endedAt;
        this.status = CouponPolicyStatus.READY;
        this.createdBy = Objects.requireNonNull(createdBy);
    }

    public static CouponPolicy create(
            String name,
            Long discountAmount,
            Long minimumOrderAmount,
            Integer totalQuantity,
            LocalDateTime startedAt,
            LocalDateTime endedAt,
            User createdBy
    ) {
        return new CouponPolicy(
                name,
                discountAmount,
                minimumOrderAmount,
                totalQuantity,
                startedAt,
                endedAt,
                createdBy
        );
    }

    public void open() {
        if (this.status != CouponPolicyStatus.READY) {
            throw new BusinessException(ErrorCode.COUPON_POLICY_STATUS_TRANSITION_INVALID);
        }
        this.status = CouponPolicyStatus.OPEN;
    }

    public void close() {
        if (this.status == CouponPolicyStatus.CLOSED) {
            throw new BusinessException(ErrorCode.COUPON_POLICY_STATUS_TRANSITION_INVALID);
        }
        this.status = CouponPolicyStatus.CLOSED;
    }

    private static void validateDiscountAmount(Long discountAmount) {
        Objects.requireNonNull(discountAmount);

        if (discountAmount <= 0) {
            throw new BusinessException(ErrorCode.COUPON_POLICY_DISCOUNT_AMOUNT_INVALID);
        }
    }

    private static void validateMinimumOrderAmount(Long minimumOrderAmount) {
        Objects.requireNonNull(minimumOrderAmount);

        if (minimumOrderAmount < 0) {
            throw new BusinessException(ErrorCode.COUPON_POLICY_MINIMUM_ORDER_AMOUNT_INVALID);
        }
    }

    private static void validateTotalQuantity(Integer totalQuantity) {
        Objects.requireNonNull(totalQuantity);

        if (totalQuantity <= 0) {
            throw new BusinessException(ErrorCode.COUPON_POLICY_TOTAL_QUANTITY_INVALID);
        }
    }

    private static void validateIssuePeriod(LocalDateTime startedAt, LocalDateTime endedAt) {
        Objects.requireNonNull(startedAt);
        Objects.requireNonNull(endedAt);

        if (!startedAt.isBefore(endedAt)) {
            throw new BusinessException(ErrorCode.COUPON_POLICY_ISSUE_PERIOD_INVALID);
        }
    }
}
