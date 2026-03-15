package com.fromvillage.coupon.application;

import com.fromvillage.coupon.domain.CouponPolicy;
import com.fromvillage.coupon.domain.IssuedCoupon;
import com.fromvillage.coupon.domain.IssuedCouponStatus;
import com.fromvillage.coupon.domain.IssuedCouponStore;
import com.fromvillage.user.domain.User;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.test.util.ReflectionTestUtils;

import java.time.LocalDateTime;
import java.util.List;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.BDDMockito.given;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.verifyNoMoreInteractions;

class CouponQueryServiceTest {

    private IssuedCouponStore issuedCouponStore;
    private CouponQueryService couponQueryService;

    @BeforeEach
    void setUp() {
        issuedCouponStore = mock(IssuedCouponStore.class);
        couponQueryService = new CouponQueryService(issuedCouponStore);
    }

    @Test
    @DisplayName("내 보유 쿠폰은 ISSUED 상태만 발급일 내림차순으로 조회한다")
    void getMyCoupons() {
        IssuedCoupon newer = createIssuedCoupon(
                11L,
                101L,
                "주말 할인",
                5000L,
                30000L,
                LocalDateTime.of(2026, 3, 15, 10, 0)
        );
        IssuedCoupon older = createIssuedCoupon(
                10L,
                100L,
                "봄맞이 할인",
                3000L,
                20000L,
                LocalDateTime.of(2026, 3, 15, 9, 0)
        );

        given(issuedCouponStore.findAllByUserIdAndStatusOrderByIssuedAtDescIdDesc(1L, IssuedCouponStatus.ISSUED))
                .willReturn(List.of(newer, older));

        CouponQueryResult result = couponQueryService.getMyCoupons(1L);

        verify(issuedCouponStore).findAllByUserIdAndStatusOrderByIssuedAtDescIdDesc(1L, IssuedCouponStatus.ISSUED);
        verifyNoMoreInteractions(issuedCouponStore);

        assertThat(result.coupons()).hasSize(2);
        assertThat(result.coupons().get(0).issuedCouponId()).isEqualTo(11L);
        assertThat(result.coupons().get(0).couponPolicyId()).isEqualTo(101L);
        assertThat(result.coupons().get(0).couponName()).isEqualTo("주말 할인");
        assertThat(result.coupons().get(0).discountAmount()).isEqualTo(5000L);
        assertThat(result.coupons().get(0).minimumOrderAmount()).isEqualTo(30000L);
        assertThat(result.coupons().get(0).status()).isEqualTo(IssuedCouponStatus.ISSUED);

        assertThat(result.coupons().get(1).issuedCouponId()).isEqualTo(10L);
        assertThat(result.coupons().get(1).couponPolicyId()).isEqualTo(100L);
        assertThat(result.coupons().get(1).couponName()).isEqualTo("봄맞이 할인");
    }

    private IssuedCoupon createIssuedCoupon(
            Long issuedCouponId,
            Long couponPolicyId,
            String couponName,
            Long discountAmount,
            Long minimumOrderAmount,
            LocalDateTime issuedAt
    ) {
        User admin = User.createAdmin("admin@example.com", "encoded-password", "운영자");
        User user = User.createUser("user@example.com", "encoded-password", "일반회원");

        CouponPolicy couponPolicy = CouponPolicy.create(
                couponName,
                discountAmount,
                minimumOrderAmount,
                100,
                LocalDateTime.of(2026, 3, 10, 0, 0),
                LocalDateTime.of(2026, 3, 31, 23, 59),
                admin
        );
        couponPolicy.open();

        ReflectionTestUtils.setField(couponPolicy, "id", couponPolicyId);

        IssuedCoupon issuedCoupon = IssuedCoupon.issue(couponPolicy, user, issuedAt);
        ReflectionTestUtils.setField(issuedCoupon, "id", issuedCouponId);
        return issuedCoupon;
    }
}
