package com.fromvillage.user.infrastructure;

import com.fromvillage.common.config.JpaAuditingConfig;
import com.fromvillage.support.TestContainersConfig;
import com.fromvillage.user.domain.User;
import com.fromvillage.user.domain.UserRole;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.orm.jpa.DataJpaTest;
import org.springframework.context.annotation.Import;
import org.springframework.dao.DataIntegrityViolationException;
import org.springframework.test.context.ActiveProfiles;

import java.time.LocalDateTime;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

@DataJpaTest
@ActiveProfiles("test")
@Import({TestContainersConfig.class, JpaAuditingConfig.class})
class UserRepositoryTest {

    @Autowired
    private UserRepository userRepository;

    @Test
    @DisplayName("사용자를 저장하고 이메일로 조회할 수 있다")
    void saveAndFindByEmail() {
        User user = User.createUser("user@example.com", "encoded-password", "fromvillage");

        User savedUser = userRepository.saveAndFlush(user);

        assertThat(savedUser.getId()).isNotNull();
        assertThat(savedUser.getRole()).isEqualTo(UserRole.USER);
        assertThat(userRepository.existsByEmail("user@example.com")).isTrue();
        assertThat(userRepository.findByEmail("user@example.com"))
                .isPresent()
                .get()
                .extracting(User::getNickname)
                .isEqualTo("fromvillage");
    }

    @Test
    @DisplayName("이메일은 unique 제약을 가진다")
    void emailMustBeUnique() {
        userRepository.saveAndFlush(User.createUser("duplicate@example.com", "encoded-password", "first"));

        assertThatThrownBy(() ->
                userRepository.saveAndFlush(User.createUser("duplicate@example.com", "encoded-password", "second"))
        ).isInstanceOf(DataIntegrityViolationException.class);
    }

    @Test
    @DisplayName("판매자 승인 시 역할과 승인 시각이 반영된다")
    void approveSellerUpdatesRoleAndSellerApprovedAt() {
        User user = userRepository.saveAndFlush(User.createUser("seller@example.com", "encoded-password", "seller"));
        LocalDateTime approvedAt = LocalDateTime.of(2026, 3, 8, 12, 0);

        user.approveSeller(approvedAt);
        User updatedUser = userRepository.saveAndFlush(user);

        assertThat(updatedUser.getRole()).isEqualTo(UserRole.SELLER);
        assertThat(updatedUser.getSellerApprovedAt()).isEqualTo(approvedAt);
    }

    @Test
    @DisplayName("사용자 저장 시 createdAt과 updatedAt이 자동 기록된다")
    void auditingFieldsAreRecordedOnSave() {
        User savedUser = userRepository.saveAndFlush(
                User.createUser("audit@example.com", "encoded-password", "auditor")
        );

        assertThat(savedUser.getCreatedAt()).isNotNull();
        assertThat(savedUser.getUpdatedAt()).isNotNull();
    }
}
