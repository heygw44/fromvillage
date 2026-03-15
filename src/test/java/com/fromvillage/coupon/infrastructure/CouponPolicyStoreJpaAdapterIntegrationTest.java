package com.fromvillage.coupon.infrastructure;

import com.fromvillage.common.config.JpaAuditingConfig;
import com.fromvillage.coupon.domain.CouponPolicy;
import com.fromvillage.coupon.domain.CouponPolicyStatus;
import com.fromvillage.coupon.domain.CouponPolicyStore;
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

import static org.assertj.core.api.Assertions.assertThat;

@DataJpaTest
@ActiveProfiles("test")
@Import({
        TestContainersConfig.class,
        JpaAuditingConfig.class,
        CouponPolicyStoreJpaAdapter.class
})
class CouponPolicyStoreJpaAdapterIntegrationTest {

    @Autowired
    private CouponPolicyStore couponPolicyStore;

    @Autowired
    private CouponPolicyJpaRepository couponPolicyJpaRepository;

    @Autowired
    private UserJpaRepository userJpaRepository;

    @Autowired
    private EntityManager entityManager;

    @Test
    @DisplayName("쿠폰 정책을 저장하면 생성자, 상태, 발급 수량, 기간 정보를 함께 보존한다")
    void saveCouponPolicy() {
        User admin = userJpaRepository.saveAndFlush(
                User.createAdmin("admin@example.com", "encoded-password", "운영자")
        );

        CouponPolicy policy = CouponPolicy.create(
                "봄맞이 할인",
                3000L,
                20000L,
                100,
                LocalDateTime.of(2026, 3, 20, 0, 0),
                LocalDateTime.of(2026, 3, 31, 23, 59),
                admin
        );

        CouponPolicy saved = couponPolicyStore.save(policy);

        assertThat(saved.getId()).isNotNull();
        assertThat(saved.getName()).isEqualTo("봄맞이 할인");
        assertThat(saved.getDiscountAmount()).isEqualTo(3000L);
        assertThat(saved.getMinimumOrderAmount()).isEqualTo(20000L);
        assertThat(saved.getTotalQuantity()).isEqualTo(100);
        assertThat(saved.getIssuedQuantity()).isEqualTo(0);
        assertThat(saved.getStatus()).isEqualTo(CouponPolicyStatus.READY);
        assertThat(saved.getStartedAt()).isEqualTo(LocalDateTime.of(2026, 3, 20, 0, 0));
        assertThat(saved.getEndedAt()).isEqualTo(LocalDateTime.of(2026, 3, 31, 23, 59));
        assertThat(saved.getCreatedBy().getId()).isEqualTo(admin.getId());
        assertThat(saved.getCreatedAt()).isNotNull();
        assertThat(saved.getUpdatedAt()).isNotNull();
    }

    @Test
    @DisplayName("저장한 쿠폰 정책은 id로 다시 조회할 수 있다")
    void findCouponPolicyById() {
        User admin = userJpaRepository.saveAndFlush(
                User.createAdmin("admin@example.com", "encoded-password", "운영자")
        );

        CouponPolicy saved = couponPolicyStore.save(CouponPolicy.create(
                "봄맞이 할인",
                3000L,
                20000L,
                100,
                LocalDateTime.of(2026, 3, 20, 0, 0),
                LocalDateTime.of(2026, 3, 31, 23, 59),
                admin
        ));

        entityManager.flush();
        entityManager.clear();

        assertThat(couponPolicyStore.findById(saved.getId()))
                .isPresent()
                .get()
                .extracting(CouponPolicy::getName, CouponPolicy::getStatus, CouponPolicy::getIssuedQuantity)
                .containsExactly("봄맞이 할인", CouponPolicyStatus.READY, 0);
    }

    @Test
    @DisplayName("존재하지 않는 id로는 쿠폰 정책이 조회되지 않는다")
    void findCouponPolicyByIdReturnsEmptyWhenMissing() {
        assertThat(couponPolicyStore.findById(Long.MAX_VALUE)).isEmpty();
    }

    @Test
    @DisplayName("쿠폰 정책을 저장하면 JPA 저장소에도 동일한 값이 반영된다")
    void saveCouponPolicyPersistsJpaFields() {
        User admin = userJpaRepository.saveAndFlush(
                User.createAdmin("admin@example.com", "encoded-password", "운영자")
        );

        CouponPolicy saved = couponPolicyStore.save(CouponPolicy.create(
                "봄맞이 할인",
                3000L,
                20000L,
                100,
                LocalDateTime.of(2026, 3, 20, 0, 0),
                LocalDateTime.of(2026, 3, 31, 23, 59),
                admin
        ));

        entityManager.flush();
        entityManager.clear();

        CouponPolicy persisted = couponPolicyJpaRepository.findById(saved.getId()).orElseThrow();

        assertThat(persisted.getCreatedBy().getId()).isEqualTo(admin.getId());
        assertThat(persisted.getStatus()).isEqualTo(CouponPolicyStatus.READY);
        assertThat(persisted.getIssuedQuantity()).isEqualTo(0);
        assertThat(persisted.getDiscountAmount()).isEqualTo(3000L);
        assertThat(persisted.getMinimumOrderAmount()).isEqualTo(20000L);
        assertThat(persisted.getTotalQuantity()).isEqualTo(100);
    }
}
