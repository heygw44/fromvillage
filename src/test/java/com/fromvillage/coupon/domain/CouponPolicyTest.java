package com.fromvillage.coupon.domain;

import com.fromvillage.common.exception.BusinessException;
import com.fromvillage.common.exception.ErrorCode;
import com.fromvillage.user.domain.User;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

import java.time.LocalDateTime;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

class CouponPolicyTest {

    @Test
    @DisplayName("쿠폰 정책을 생성하면 READY 상태와 발급 수량 0으로 시작한다")
    void createCouponPolicy() {
        User admin = User.createAdmin("admin@example.com", "encoded-password", "운영자");

        LocalDateTime startedAt = LocalDateTime.of(2026, 3, 20, 0, 0);
        LocalDateTime endedAt = LocalDateTime.of(2026, 3, 31, 23, 59);

        CouponPolicy policy = CouponPolicy.create(
                "봄맞이 할인",
                3000L,
                20000L,
                100,
                startedAt,
                endedAt,
                admin
        );

        assertThat(policy.getName()).isEqualTo("봄맞이 할인");
        assertThat(policy.getDiscountAmount()).isEqualTo(3000L);
        assertThat(policy.getMinimumOrderAmount()).isEqualTo(20000L);
        assertThat(policy.getTotalQuantity()).isEqualTo(100);
        assertThat(policy.getIssuedQuantity()).isEqualTo(0);
        assertThat(policy.getStartedAt()).isEqualTo(startedAt);
        assertThat(policy.getEndedAt()).isEqualTo(endedAt);
        assertThat(policy.getCreatedBy()).isEqualTo(admin);
        assertThat(policy.getStatus()).isEqualTo(CouponPolicyStatus.READY);
    }

    @Test
    @DisplayName("발급 시작 시각이 종료 시각과 같거나 늦으면 쿠폰 정책을 생성할 수 없다")
    void createCouponPolicyRejectsInvalidIssuePeriod() {
        User admin = User.createAdmin("admin@example.com", "encoded-password", "운영자");

        LocalDateTime startedAt = LocalDateTime.of(2026, 3, 31, 23, 59);
        LocalDateTime endedAt = LocalDateTime.of(2026, 3, 31, 23, 59);

        assertThatThrownBy(() -> CouponPolicy.create(
                "봄맞이 할인",
                3000L,
                20000L,
                100,
                startedAt,
                endedAt,
                admin
        )).isInstanceOf(BusinessException.class)
                .extracting(exception -> ((BusinessException) exception).getErrorCode())
                .isEqualTo(ErrorCode.COUPON_POLICY_ISSUE_PERIOD_INVALID);
    }

    @Test
    @DisplayName("할인 금액이 0 이하이면 쿠폰 정책을 생성할 수 없다")
    void createCouponPolicyRejectsNonPositiveDiscountAmount() {
        User admin = User.createAdmin("admin@example.com", "encoded-password", "운영자");

        assertThatThrownBy(() -> CouponPolicy.create(
                "봄맞이 할인",
                0L,
                20000L,
                100,
                LocalDateTime.of(2026, 3, 20, 0, 0),
                LocalDateTime.of(2026, 3, 31, 23, 59),
                admin
        )).isInstanceOf(BusinessException.class)
                .extracting(exception -> ((BusinessException) exception).getErrorCode())
                .isEqualTo(ErrorCode.COUPON_POLICY_DISCOUNT_AMOUNT_INVALID);
    }

    @Test
    @DisplayName("최소 주문 금액이 0 미만이면 쿠폰 정책을 생성할 수 없다")
    void createCouponPolicyRejectsNegativeMinimumOrderAmount() {
        User admin = User.createAdmin("admin@example.com", "encoded-password", "운영자");

        assertThatThrownBy(() -> CouponPolicy.create(
                "봄맞이 할인",
                3000L,
                -1L,
                100,
                LocalDateTime.of(2026, 3, 20, 0, 0),
                LocalDateTime.of(2026, 3, 31, 23, 59),
                admin
        )).isInstanceOf(BusinessException.class)
                .extracting(exception -> ((BusinessException) exception).getErrorCode())
                .isEqualTo(ErrorCode.COUPON_POLICY_MINIMUM_ORDER_AMOUNT_INVALID);
    }

    @Test
    @DisplayName("총 발급 수량이 0 이하이면 쿠폰 정책을 생성할 수 없다")
    void createCouponPolicyRejectsNonPositiveTotalQuantity() {
        User admin = User.createAdmin("admin@example.com", "encoded-password", "운영자");

        assertThatThrownBy(() -> CouponPolicy.create(
                "봄맞이 할인",
                3000L,
                20000L,
                0,
                LocalDateTime.of(2026, 3, 20, 0, 0),
                LocalDateTime.of(2026, 3, 31, 23, 59),
                admin
        )).isInstanceOf(BusinessException.class)
                .extracting(exception -> ((BusinessException) exception).getErrorCode())
                .isEqualTo(ErrorCode.COUPON_POLICY_TOTAL_QUANTITY_INVALID);
    }

    @Test
    @DisplayName("READY 상태의 쿠폰 정책은 OPEN으로 전이할 수 있다")
    void openCouponPolicy() {
        CouponPolicy policy = createPolicy();

        policy.open();

        assertThat(policy.getStatus()).isEqualTo(CouponPolicyStatus.OPEN);
    }

    @Test
    @DisplayName("READY 상태의 쿠폰 정책은 CLOSED로 전이할 수 있다")
    void closeReadyCouponPolicy() {
        CouponPolicy policy = createPolicy();

        policy.close();

        assertThat(policy.getStatus()).isEqualTo(CouponPolicyStatus.CLOSED);
    }

    @Test
    @DisplayName("OPEN 상태의 쿠폰 정책은 CLOSED로 전이할 수 있다")
    void closeOpenCouponPolicy() {
        CouponPolicy policy = createPolicy();
        policy.open();

        policy.close();

        assertThat(policy.getStatus()).isEqualTo(CouponPolicyStatus.CLOSED);
    }

    @Test
    @DisplayName("이미 OPEN인 쿠폰 정책은 다시 OPEN으로 전이할 수 없다")
    void openCouponPolicyRejectsAlreadyOpen() {
        CouponPolicy policy = createPolicy();
        policy.open();

        assertThatThrownBy(policy::open)
                .isInstanceOf(BusinessException.class)
                .extracting(exception -> ((BusinessException) exception).getErrorCode())
                .isEqualTo(ErrorCode.COUPON_POLICY_STATUS_TRANSITION_INVALID);
    }

    @Test
    @DisplayName("CLOSED 상태의 쿠폰 정책은 다시 OPEN으로 전이할 수 없다")
    void openCouponPolicyRejectsClosedPolicy() {
        CouponPolicy policy = createPolicy();
        policy.close();

        assertThatThrownBy(policy::open)
                .isInstanceOf(BusinessException.class)
                .extracting(exception -> ((BusinessException) exception).getErrorCode())
                .isEqualTo(ErrorCode.COUPON_POLICY_STATUS_TRANSITION_INVALID);
    }

    @Test
    @DisplayName("이미 CLOSED인 쿠폰 정책은 다시 CLOSED로 전이할 수 없다")
    void closeCouponPolicyRejectsAlreadyClosed() {
        CouponPolicy policy = createPolicy();
        policy.close();

        assertThatThrownBy(policy::close)
                .isInstanceOf(BusinessException.class)
                .extracting(exception -> ((BusinessException) exception).getErrorCode())
                .isEqualTo(ErrorCode.COUPON_POLICY_STATUS_TRANSITION_INVALID);
    }

    private CouponPolicy createPolicy() {
        User admin = User.createAdmin("admin@example.com", "encoded-password", "운영자");

        return CouponPolicy.create(
                "봄맞이 할인",
                3000L,
                20000L,
                100,
                LocalDateTime.of(2026, 3, 20, 0, 0),
                LocalDateTime.of(2026, 3, 31, 23, 59),
                admin
        );
    }
}
