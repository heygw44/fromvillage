package com.fromvillage.coupon.infrastructure;

import com.fromvillage.common.config.JpaAuditingConfig;
import com.fromvillage.coupon.domain.CouponPolicy;
import com.fromvillage.coupon.domain.CouponPolicyStatus;
import com.fromvillage.coupon.domain.IssuedCoupon;
import com.fromvillage.coupon.domain.IssuedCouponStatus;
import com.fromvillage.coupon.domain.IssuedCouponStore;
import com.fromvillage.support.TestContainersConfig;
import com.fromvillage.user.domain.User;
import com.fromvillage.user.infrastructure.UserJpaRepository;
import jakarta.persistence.EntityManager;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.orm.jpa.DataJpaTest;
import org.springframework.context.annotation.Import;
import org.springframework.test.context.ActiveProfiles;

import java.time.LocalDateTime;
import java.util.List;

import static org.assertj.core.api.Assertions.assertThat;

@DataJpaTest
@ActiveProfiles("test")
@Import({
        TestContainersConfig.class,
        JpaAuditingConfig.class,
        CouponPolicyStoreJpaAdapter.class,
        IssuedCouponStoreJpaAdapter.class
})
class IssuedCouponStoreJpaAdapterIntegrationTest {

    @Autowired
    private IssuedCouponStore issuedCouponStore;

    @Autowired
    private CouponPolicyJpaRepository couponPolicyJpaRepository;

    @Autowired
    private UserJpaRepository userJpaRepository;

    @Autowired
    private EntityManager entityManager;

    @Test
    @DisplayName("발급 쿠폰을 저장하면 정책, 사용자, 상태, 발급 시각을 함께 보존한다")
    void saveIssuedCoupon() {
        User admin = userJpaRepository.saveAndFlush(
                User.createAdmin("admin@example.com", "encoded-password", "운영자")
        );
        User user = userJpaRepository.saveAndFlush(
                User.createUser("user@example.com", "encoded-password", "일반회원")
        );
        CouponPolicy couponPolicy = couponPolicyJpaRepository.saveAndFlush(createOpenPolicy(admin));

        IssuedCoupon issuedCoupon = IssuedCoupon.issue(
                couponPolicy,
                user,
                LocalDateTime.of(2026, 3, 15, 10, 0)
        );

        IssuedCoupon saved = issuedCouponStore.save(issuedCoupon);

        assertThat(saved.getId()).isNotNull();
        assertThat(saved.getCouponPolicy().getId()).isEqualTo(couponPolicy.getId());
        assertThat(saved.getUser().getId()).isEqualTo(user.getId());
        assertThat(saved.getStatus()).isEqualTo(IssuedCouponStatus.ISSUED);
        assertThat(saved.getIssuedAt()).isEqualTo(LocalDateTime.of(2026, 3, 15, 10, 0));
        assertThat(saved.getUsedAt()).isNull();
        assertThat(saved.getCreatedAt()).isNotNull();
        assertThat(saved.getUpdatedAt()).isNotNull();
    }

    @Test
    @DisplayName("같은 사용자와 쿠폰 정책 조합의 발급 이력이 있으면 중복 발급 여부를 확인할 수 있다")
    void existsByCouponPolicyIdAndUserId() {
        User admin = userJpaRepository.saveAndFlush(
                User.createAdmin("admin@example.com", "encoded-password", "운영자")
        );
        User user = userJpaRepository.saveAndFlush(
                User.createUser("user@example.com", "encoded-password", "일반회원")
        );
        CouponPolicy couponPolicy = couponPolicyJpaRepository.saveAndFlush(createOpenPolicy(admin));

        issuedCouponStore.save(IssuedCoupon.issue(
                couponPolicy,
                user,
                LocalDateTime.of(2026, 3, 15, 10, 0)
        ));

        entityManager.flush();
        entityManager.clear();

        assertThat(issuedCouponStore.existsByCouponPolicyIdAndUserId(couponPolicy.getId(), user.getId())).isTrue();
        assertThat(issuedCouponStore.existsByCouponPolicyIdAndUserId(couponPolicy.getId(), Long.MAX_VALUE)).isFalse();
    }

    @Test
    @DisplayName("사용자의 ISSUED 쿠폰만 발급 시각 내림차순, id 내림차순으로 조회한다")
    void findAllByUserIdAndStatusOrderByIssuedAtDescIdDesc() {
        User admin = userJpaRepository.saveAndFlush(
                User.createAdmin("admin@example.com", "encoded-password", "운영자")
        );
        User user = userJpaRepository.saveAndFlush(
                User.createUser("user@example.com", "encoded-password", "일반회원")
        );
        User otherUser = userJpaRepository.saveAndFlush(
                User.createUser("other@example.com", "encoded-password", "다른회원")
        );

        CouponPolicy firstPolicy = couponPolicyJpaRepository.saveAndFlush(createOpenPolicy(
                admin,
                "봄맞이 할인",
                3000L,
                20000L,
                100
        ));
        CouponPolicy secondPolicy = couponPolicyJpaRepository.saveAndFlush(createOpenPolicy(
                admin,
                "주말 할인",
                5000L,
                30000L,
                50
        ));

        IssuedCoupon olderIssued = issuedCouponStore.save(IssuedCoupon.issue(
                firstPolicy,
                user,
                LocalDateTime.of(2026, 3, 15, 9, 0)
        ));

        IssuedCoupon newerIssued = issuedCouponStore.save(IssuedCoupon.issue(
                secondPolicy,
                user,
                LocalDateTime.of(2026, 3, 15, 10, 0)
        ));

        IssuedCoupon usedCoupon = issuedCouponStore.save(IssuedCoupon.issue(
                firstPolicy,
                user,
                LocalDateTime.of(2026, 3, 15, 11, 0)
        ));
        usedCoupon.use(LocalDateTime.of(2026, 3, 16, 12, 0));

        issuedCouponStore.save(IssuedCoupon.issue(
                firstPolicy,
                otherUser,
                LocalDateTime.of(2026, 3, 15, 12, 0)
        ));

        entityManager.flush();
        entityManager.clear();

        List<IssuedCoupon> coupons = issuedCouponStore.findAllByUserIdAndStatusOrderByIssuedAtDescIdDesc(
                user.getId(),
                IssuedCouponStatus.ISSUED
        );

        assertThat(coupons).hasSize(2);
        assertThat(coupons)
                .extracting(IssuedCoupon::getId)
                .containsExactly(newerIssued.getId(), olderIssued.getId());
        assertThat(coupons)
                .extracting(coupon -> coupon.getCouponPolicy().getName())
                .containsExactly("주말 할인", "봄맞이 할인");
    }

    private CouponPolicy createOpenPolicy(User admin) {
        return createOpenPolicy(admin, "봄맞이 할인", 3000L, 20000L, 100);
    }

    private CouponPolicy createOpenPolicy(
            User admin,
            String name,
            Long discountAmount,
            Long minimumOrderAmount,
            Integer totalQuantity
    ) {
        CouponPolicy couponPolicy = CouponPolicy.create(
                name,
                discountAmount,
                minimumOrderAmount,
                totalQuantity,
                LocalDateTime.of(2026, 3, 10, 0, 0),
                LocalDateTime.of(2026, 3, 31, 23, 59),
                admin
        );
        couponPolicy.open();
        return couponPolicy;
    }
}
