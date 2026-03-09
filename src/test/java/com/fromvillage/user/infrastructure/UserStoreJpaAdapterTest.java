package com.fromvillage.user.infrastructure;

import com.fromvillage.common.config.JpaAuditingConfig;
import com.fromvillage.common.exception.BusinessException;
import com.fromvillage.common.exception.ErrorCode;
import com.fromvillage.support.TestContainersConfig;
import com.fromvillage.user.domain.User;
import com.fromvillage.user.domain.UserStore;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.orm.jpa.DataJpaTest;
import org.springframework.context.annotation.Import;
import org.springframework.test.context.ActiveProfiles;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

@DataJpaTest
@ActiveProfiles("test")
@Import({TestContainersConfig.class, JpaAuditingConfig.class, UserStoreJpaAdapter.class})
class UserStoreJpaAdapterTest {

    @Autowired
    private UserStore userStore;

    @Autowired
    private UserJpaRepository userJpaRepository;

    @Test
    @DisplayName("어댑터는 사용자를 저장하고 이메일 존재 여부를 조회할 수 있다")
    void saveAndExistsByEmail() {
        User savedUser = userStore.save(User.createUser("user@example.com", "encoded-password", "fromvillage"));

        assertThat(savedUser.getId()).isNotNull();
        assertThat(userStore.existsByEmail("user@example.com")).isTrue();
        assertThat(userStore.findByEmail("user@example.com")).contains(savedUser);
        assertThat(userStore.findById(savedUser.getId())).contains(savedUser);
        assertThat(userStore.findByEmail("missing@example.com")).isEmpty();
        assertThat(userStore.findById(-1L)).isEmpty();
    }

    @Test
    @DisplayName("이메일 unique 위반은 중복 이메일 비즈니스 예외로 변환한다")
    void saveMapsDuplicateEmailConstraint() {
        userJpaRepository.saveAndFlush(User.createUser("duplicate@example.com", "encoded-password", "first"));

        assertThatThrownBy(() -> userStore.save(
                User.createUser("duplicate@example.com", "encoded-password", "second")
        )).isInstanceOf(BusinessException.class)
                .extracting(exception -> ((BusinessException) exception).getErrorCode())
                .isEqualTo(ErrorCode.USER_EMAIL_ALREADY_EXISTS);
    }

}
