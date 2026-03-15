package com.fromvillage.coupon.application;

import com.fromvillage.common.exception.BusinessException;
import com.fromvillage.common.exception.ErrorCode;
import com.fromvillage.coupon.domain.CouponPolicy;
import com.fromvillage.coupon.domain.CouponPolicyStatus;
import com.fromvillage.coupon.domain.CouponPolicyStore;
import com.fromvillage.user.domain.User;
import com.fromvillage.user.domain.UserStore;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.test.util.ReflectionTestUtils;

import java.time.LocalDateTime;
import java.util.Optional;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.BDDMockito.given;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.verifyNoMoreInteractions;

@ExtendWith(MockitoExtension.class)
class AdminCouponPolicyServiceTest {

    @Mock
    private CouponPolicyStore couponPolicyStore;

    @Mock
    private UserStore userStore;

    private AdminCouponPolicyService adminCouponPolicyService;

    @BeforeEach
    void setUp() {
        adminCouponPolicyService = new AdminCouponPolicyService(couponPolicyStore, userStore);
    }

    @Test
    @DisplayName("관리자는 쿠폰 정책을 생성할 수 있다")
    void createCouponPolicy() {
        User admin = User.createAdmin("admin@example.com", "encoded-password", "운영자");
        ReflectionTestUtils.setField(admin, "id", 1L);

        AdminCouponPolicyCreateCommand command = new AdminCouponPolicyCreateCommand(
                "봄맞이 할인",
                3000L,
                20000L,
                100,
                LocalDateTime.of(2026, 3, 20, 0, 0),
                LocalDateTime.of(2026, 3, 31, 23, 59)
        );

        given(userStore.findById(1L)).willReturn(Optional.of(admin));
        given(couponPolicyStore.save(any(CouponPolicy.class))).willAnswer(invocation -> {
            CouponPolicy couponPolicy = invocation.getArgument(0);
            ReflectionTestUtils.setField(couponPolicy, "id", 10L);
            return couponPolicy;
        });

        AdminCouponPolicyResult result = adminCouponPolicyService.createCouponPolicy(1L, command);

        verify(couponPolicyStore).save(any(CouponPolicy.class));
        assertThat(result.couponPolicyId()).isEqualTo(10L);
        assertThat(result.name()).isEqualTo("봄맞이 할인");
        assertThat(result.discountAmount()).isEqualTo(3000L);
        assertThat(result.minimumOrderAmount()).isEqualTo(20000L);
        assertThat(result.totalQuantity()).isEqualTo(100);
        assertThat(result.issuedQuantity()).isEqualTo(0);
        assertThat(result.status()).isEqualTo(CouponPolicyStatus.READY);
        assertThat(result.startedAt()).isEqualTo(LocalDateTime.of(2026, 3, 20, 0, 0));
        assertThat(result.endedAt()).isEqualTo(LocalDateTime.of(2026, 3, 31, 23, 59));
    }

    @Test
    @DisplayName("존재하지 않는 관리자면 USER_NOT_FOUND 예외를 반환한다")
    void createCouponPolicyRejectsMissingAdmin() {
        AdminCouponPolicyCreateCommand command = new AdminCouponPolicyCreateCommand(
                "봄맞이 할인",
                3000L,
                20000L,
                100,
                LocalDateTime.of(2026, 3, 20, 0, 0),
                LocalDateTime.of(2026, 3, 31, 23, 59)
        );

        given(userStore.findById(1L)).willReturn(Optional.empty());

        assertThatThrownBy(() -> adminCouponPolicyService.createCouponPolicy(1L, command))
                .isInstanceOf(BusinessException.class)
                .extracting(exception -> ((BusinessException) exception).getErrorCode())
                .isEqualTo(ErrorCode.USER_NOT_FOUND);
    }

    @Test
    @DisplayName("관리자는 READY 상태의 쿠폰 정책을 OPEN으로 전이할 수 있다")
    void openCouponPolicy() {
        CouponPolicy couponPolicy = createPolicy();
        ReflectionTestUtils.setField(couponPolicy, "id", 10L);

        given(couponPolicyStore.findById(10L)).willReturn(Optional.of(couponPolicy));

        AdminCouponPolicyResult result = adminCouponPolicyService.openCouponPolicy(10L);

        verify(couponPolicyStore).findById(10L);
        verifyNoMoreInteractions(couponPolicyStore);
        assertThat(result.couponPolicyId()).isEqualTo(10L);
        assertThat(result.status()).isEqualTo(CouponPolicyStatus.OPEN);
    }

    @Test
    @DisplayName("관리자는 READY 상태의 쿠폰 정책을 CLOSED로 전이할 수 있다")
    void closeReadyCouponPolicy() {
        CouponPolicy couponPolicy = createPolicy();
        ReflectionTestUtils.setField(couponPolicy, "id", 10L);

        given(couponPolicyStore.findById(10L)).willReturn(Optional.of(couponPolicy));

        AdminCouponPolicyResult result = adminCouponPolicyService.closeCouponPolicy(10L);

        verify(couponPolicyStore).findById(10L);
        verifyNoMoreInteractions(couponPolicyStore);
        assertThat(result.couponPolicyId()).isEqualTo(10L);
        assertThat(result.status()).isEqualTo(CouponPolicyStatus.CLOSED);
    }

    @Test
    @DisplayName("관리자는 OPEN 상태의 쿠폰 정책을 CLOSED로 전이할 수 있다")
    void closeOpenCouponPolicy() {
        CouponPolicy couponPolicy = createPolicy();
        ReflectionTestUtils.setField(couponPolicy, "id", 10L);
        couponPolicy.open();

        given(couponPolicyStore.findById(10L)).willReturn(Optional.of(couponPolicy));

        AdminCouponPolicyResult result = adminCouponPolicyService.closeCouponPolicy(10L);

        verify(couponPolicyStore).findById(10L);
        verifyNoMoreInteractions(couponPolicyStore);
        assertThat(result.couponPolicyId()).isEqualTo(10L);
        assertThat(result.status()).isEqualTo(CouponPolicyStatus.CLOSED);
    }

    @Test
    @DisplayName("존재하지 않는 쿠폰 정책을 오픈하면 COUPON_POLICY_NOT_FOUND 예외를 반환한다")
    void openCouponPolicyRejectsMissingPolicy() {
        given(couponPolicyStore.findById(10L)).willReturn(Optional.empty());

        assertThatThrownBy(() -> adminCouponPolicyService.openCouponPolicy(10L))
                .isInstanceOf(BusinessException.class)
                .extracting(exception -> ((BusinessException) exception).getErrorCode())
                .isEqualTo(ErrorCode.COUPON_POLICY_NOT_FOUND);
    }

    @Test
    @DisplayName("존재하지 않는 쿠폰 정책을 종료하면 COUPON_POLICY_NOT_FOUND 예외를 반환한다")
    void closeCouponPolicyRejectsMissingPolicy() {
        given(couponPolicyStore.findById(10L)).willReturn(Optional.empty());

        assertThatThrownBy(() -> adminCouponPolicyService.closeCouponPolicy(10L))
                .isInstanceOf(BusinessException.class)
                .extracting(exception -> ((BusinessException) exception).getErrorCode())
                .isEqualTo(ErrorCode.COUPON_POLICY_NOT_FOUND);
    }

    private CouponPolicy createPolicy() {
        User admin = User.createAdmin("admin@example.com", "encoded-password", "운영자");
        ReflectionTestUtils.setField(admin, "id", 1L);

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
