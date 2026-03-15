package com.fromvillage.coupon.application;

import com.fromvillage.common.exception.BusinessException;
import com.fromvillage.common.exception.ErrorCode;
import com.fromvillage.coupon.domain.CouponPolicy;
import com.fromvillage.coupon.domain.CouponPolicyStore;
import com.fromvillage.coupon.domain.IssuedCoupon;
import com.fromvillage.coupon.domain.IssuedCouponStatus;
import com.fromvillage.coupon.domain.IssuedCouponStore;
import com.fromvillage.user.domain.User;
import com.fromvillage.user.domain.UserStore;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.test.util.ReflectionTestUtils;

import java.time.Clock;
import java.time.Instant;
import java.time.LocalDateTime;
import java.time.ZoneId;
import java.util.Optional;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.BDDMockito.given;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.verifyNoMoreInteractions;

class CouponIssueServiceTest {

    private UserStore userStore;
    private CouponPolicyStore couponPolicyStore;
    private IssuedCouponStore issuedCouponStore;
    private Clock clock;

    private CouponIssueService couponIssueService;

    @BeforeEach
    void setUp() {
        userStore = mock(UserStore.class);
        couponPolicyStore = mock(CouponPolicyStore.class);
        issuedCouponStore = mock(IssuedCouponStore.class);
        clock = Clock.fixed(
                Instant.parse("2026-03-15T01:00:00Z"),
                ZoneId.of("Asia/Seoul")
        );

        couponIssueService = new CouponIssueService(
                userStore,
                couponPolicyStore,
                issuedCouponStore,
                clock
        );
    }

    @Test
    @DisplayName("USER는 OPEN 상태의 쿠폰을 발급받을 수 있다")
    void issueCoupon() {
        User user = createUser();
        CouponPolicy couponPolicy = createOpenPolicy();
        ReflectionTestUtils.setField(couponPolicy, "id", 10L);

        given(userStore.findById(1L)).willReturn(Optional.of(user));
        given(couponPolicyStore.findById(10L)).willReturn(Optional.of(couponPolicy));
        given(issuedCouponStore.existsByCouponPolicyIdAndUserId(10L, 1L)).willReturn(false);
        given(issuedCouponStore.save(any(IssuedCoupon.class))).willAnswer(invocation -> {
            IssuedCoupon issuedCoupon = invocation.getArgument(0);
            ReflectionTestUtils.setField(issuedCoupon, "id", 100L);
            return issuedCoupon;
        });

        CouponIssueResult result = couponIssueService.issue(1L, 10L);

        verify(userStore).findById(1L);
        verify(couponPolicyStore).findById(10L);
        verify(issuedCouponStore).existsByCouponPolicyIdAndUserId(10L, 1L);
        verify(issuedCouponStore).save(any(IssuedCoupon.class));
        verifyNoMoreInteractions(userStore, couponPolicyStore, issuedCouponStore);

        assertThat(result.issuedCouponId()).isEqualTo(100L);
        assertThat(result.couponPolicyId()).isEqualTo(10L);
        assertThat(result.couponName()).isEqualTo("봄맞이 할인");
        assertThat(result.discountAmount()).isEqualTo(3000L);
        assertThat(result.minimumOrderAmount()).isEqualTo(20000L);
        assertThat(result.status()).isEqualTo(IssuedCouponStatus.ISSUED);
        assertThat(result.issuedAt()).isEqualTo(LocalDateTime.of(2026, 3, 15, 10, 0));
        assertThat(result.startedAt()).isEqualTo(LocalDateTime.of(2026, 3, 10, 0, 0));
        assertThat(result.endedAt()).isEqualTo(LocalDateTime.of(2026, 3, 31, 23, 59));
        assertThat(couponPolicy.getIssuedQuantity()).isEqualTo(1);
    }

    @Test
    @DisplayName("이미 같은 쿠폰 정책을 발급받은 사용자는 다시 발급받을 수 없다")
    void issueCouponFailsWhenAlreadyIssued() {
        User user = createUser();
        CouponPolicy couponPolicy = createOpenPolicy();
        ReflectionTestUtils.setField(couponPolicy, "id", 10L);

        given(userStore.findById(1L)).willReturn(Optional.of(user));
        given(couponPolicyStore.findById(10L)).willReturn(Optional.of(couponPolicy));
        given(issuedCouponStore.existsByCouponPolicyIdAndUserId(10L, 1L)).willReturn(true);

        assertThatThrownBy(() -> couponIssueService.issue(1L, 10L))
                .isInstanceOf(BusinessException.class)
                .extracting("errorCode")
                .isEqualTo(ErrorCode.COUPON_ALREADY_ISSUED);

        verify(userStore).findById(1L);
        verify(couponPolicyStore).findById(10L);
        verify(issuedCouponStore).existsByCouponPolicyIdAndUserId(10L, 1L);
        verifyNoMoreInteractions(userStore, couponPolicyStore, issuedCouponStore);
    }

    @Test
    @DisplayName("존재하지 않는 사용자는 쿠폰을 발급받을 수 없다")
    void issueCouponFailsWhenUserMissing() {
        given(userStore.findById(1L)).willReturn(Optional.empty());

        assertThatThrownBy(() -> couponIssueService.issue(1L, 10L))
                .isInstanceOf(BusinessException.class)
                .extracting("errorCode")
                .isEqualTo(ErrorCode.USER_NOT_FOUND);

        verify(userStore).findById(1L);
        verifyNoMoreInteractions(userStore, couponPolicyStore, issuedCouponStore);
    }

    @Test
    @DisplayName("존재하지 않는 쿠폰 정책은 발급할 수 없다")
    void issueCouponFailsWhenCouponPolicyMissing() {
        User user = createUser();

        given(userStore.findById(1L)).willReturn(Optional.of(user));
        given(couponPolicyStore.findById(10L)).willReturn(Optional.empty());

        assertThatThrownBy(() -> couponIssueService.issue(1L, 10L))
                .isInstanceOf(BusinessException.class)
                .extracting("errorCode")
                .isEqualTo(ErrorCode.COUPON_POLICY_NOT_FOUND);

        verify(userStore).findById(1L);
        verify(couponPolicyStore).findById(10L);
        verifyNoMoreInteractions(userStore, couponPolicyStore, issuedCouponStore);
    }

    @Test
    @DisplayName("OPEN 상태가 아닌 쿠폰 정책은 발급할 수 없다")
    void issueCouponFailsWhenCouponPolicyNotOpen() {
        User user = createUser();
        CouponPolicy couponPolicy = CouponPolicy.create(
                "봄맞이 할인",
                3000L,
                20000L,
                100,
                LocalDateTime.of(2026, 3, 10, 0, 0),
                LocalDateTime.of(2026, 3, 31, 23, 59),
                createAdmin()
        );
        ReflectionTestUtils.setField(couponPolicy, "id", 10L);

        given(userStore.findById(1L)).willReturn(Optional.of(user));
        given(couponPolicyStore.findById(10L)).willReturn(Optional.of(couponPolicy));
        given(issuedCouponStore.existsByCouponPolicyIdAndUserId(10L, 1L)).willReturn(false);

        assertThatThrownBy(() -> couponIssueService.issue(1L, 10L))
                .isInstanceOf(BusinessException.class)
                .extracting("errorCode")
                .isEqualTo(ErrorCode.COUPON_POLICY_NOT_OPEN);

        verify(userStore).findById(1L);
        verify(couponPolicyStore).findById(10L);
        verify(issuedCouponStore).existsByCouponPolicyIdAndUserId(10L, 1L);
        verifyNoMoreInteractions(userStore, couponPolicyStore, issuedCouponStore);
    }

    @Test
    @DisplayName("발급 기간이 아닌 쿠폰 정책은 발급할 수 없다")
    void issueCouponFailsWhenIssuePeriodNotActive() {
        User user = createUser();
        CouponPolicy couponPolicy = CouponPolicy.create(
                "봄맞이 할인",
                3000L,
                20000L,
                100,
                LocalDateTime.of(2026, 3, 16, 0, 0),
                LocalDateTime.of(2026, 3, 31, 23, 59),
                createAdmin()
        );
        couponPolicy.open();
        ReflectionTestUtils.setField(couponPolicy, "id", 10L);

        given(userStore.findById(1L)).willReturn(Optional.of(user));
        given(couponPolicyStore.findById(10L)).willReturn(Optional.of(couponPolicy));
        given(issuedCouponStore.existsByCouponPolicyIdAndUserId(10L, 1L)).willReturn(false);

        assertThatThrownBy(() -> couponIssueService.issue(1L, 10L))
                .isInstanceOf(BusinessException.class)
                .extracting("errorCode")
                .isEqualTo(ErrorCode.COUPON_POLICY_ISSUE_PERIOD_NOT_ACTIVE);

        verify(userStore).findById(1L);
        verify(couponPolicyStore).findById(10L);
        verify(issuedCouponStore).existsByCouponPolicyIdAndUserId(10L, 1L);
        verifyNoMoreInteractions(userStore, couponPolicyStore, issuedCouponStore);
    }

    @Test
    @DisplayName("발급 수량이 모두 소진된 쿠폰 정책은 발급할 수 없다")
    void issueCouponFailsWhenQuantityExhausted() {
        User user = createUser();
        CouponPolicy couponPolicy = CouponPolicy.create(
                "봄맞이 할인",
                3000L,
                20000L,
                1,
                LocalDateTime.of(2026, 3, 10, 0, 0),
                LocalDateTime.of(2026, 3, 31, 23, 59),
                createAdmin()
        );
        couponPolicy.open();
        couponPolicy.issue(LocalDateTime.of(2026, 3, 14, 10, 0));
        ReflectionTestUtils.setField(couponPolicy, "id", 10L);

        given(userStore.findById(1L)).willReturn(Optional.of(user));
        given(couponPolicyStore.findById(10L)).willReturn(Optional.of(couponPolicy));
        given(issuedCouponStore.existsByCouponPolicyIdAndUserId(10L, 1L)).willReturn(false);

        assertThatThrownBy(() -> couponIssueService.issue(1L, 10L))
                .isInstanceOf(BusinessException.class)
                .extracting("errorCode")
                .isEqualTo(ErrorCode.COUPON_POLICY_QUANTITY_EXHAUSTED);

        verify(userStore).findById(1L);
        verify(couponPolicyStore).findById(10L);
        verify(issuedCouponStore).existsByCouponPolicyIdAndUserId(10L, 1L);
        verifyNoMoreInteractions(userStore, couponPolicyStore, issuedCouponStore);
    }

    private User createUser() {
        User user = User.createUser("user@example.com", "encoded-password", "일반회원");
        ReflectionTestUtils.setField(user, "id", 1L);
        return user;
    }

    private User createAdmin() {
        User admin = User.createAdmin("admin@example.com", "encoded-password", "운영자");
        ReflectionTestUtils.setField(admin, "id", 99L);
        return admin;
    }

    private CouponPolicy createOpenPolicy() {
        CouponPolicy couponPolicy = CouponPolicy.create(
                "봄맞이 할인",
                3000L,
                20000L,
                100,
                LocalDateTime.of(2026, 3, 10, 0, 0),
                LocalDateTime.of(2026, 3, 31, 23, 59),
                createAdmin()
        );
        couponPolicy.open();
        return couponPolicy;
    }
}
