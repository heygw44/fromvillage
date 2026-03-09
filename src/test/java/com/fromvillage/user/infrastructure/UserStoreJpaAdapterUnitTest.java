package com.fromvillage.user.infrastructure;

import com.fromvillage.common.exception.BusinessException;
import com.fromvillage.common.exception.ErrorCode;
import com.fromvillage.user.domain.User;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.dao.DataIntegrityViolationException;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.BDDMockito.given;

@ExtendWith(MockitoExtension.class)
class UserStoreJpaAdapterUnitTest {

    @Mock
    private UserJpaRepository userJpaRepository;

    @InjectMocks
    private UserStoreJpaAdapter userStoreJpaAdapter;

    @Test
    @DisplayName("사용자 저장에 성공하면 저장된 엔티티를 그대로 반환한다")
    void saveReturnsUserOnSuccess() {
        User user = User.createUser("user@example.com", "encoded-password", "nickname");
        given(userJpaRepository.saveAndFlush(user)).willReturn(user);

        User savedUser = userStoreJpaAdapter.save(user);

        assertThat(savedUser).isSameAs(user);
    }

    @Test
    @DisplayName("이메일 존재 여부 조회 결과를 그대로 반환한다")
    void existsByEmailReturnsRepositoryResult() {
        given(userJpaRepository.existsByEmail("exists@example.com")).willReturn(true);
        given(userJpaRepository.existsByEmail("missing@example.com")).willReturn(false);

        assertThat(userStoreJpaAdapter.existsByEmail("exists@example.com")).isTrue();
        assertThat(userStoreJpaAdapter.existsByEmail("missing@example.com")).isFalse();
    }

    @Test
    @DisplayName("이메일로 사용자를 조회할 때 저장소 결과를 그대로 반환한다")
    void findByEmailReturnsRepositoryResult() {
        User user = User.createUser("user@example.com", "encoded-password", "nickname");
        given(userJpaRepository.findByEmail("user@example.com")).willReturn(java.util.Optional.of(user));
        given(userJpaRepository.findByEmail("missing@example.com")).willReturn(java.util.Optional.empty());

        assertThat(userStoreJpaAdapter.findByEmail("user@example.com")).contains(user);
        assertThat(userStoreJpaAdapter.findByEmail("missing@example.com")).isEmpty();
    }

    @Test
    @DisplayName("ID로 사용자를 조회할 때 저장소 결과를 그대로 반환한다")
    void findByIdReturnsRepositoryResult() {
        User user = User.createUser("user@example.com", "encoded-password", "nickname");
        given(userJpaRepository.findById(1L)).willReturn(java.util.Optional.of(user));
        given(userJpaRepository.findById(999L)).willReturn(java.util.Optional.empty());

        assertThat(userStoreJpaAdapter.findById(1L)).contains(user);
        assertThat(userStoreJpaAdapter.findById(999L)).isEmpty();
    }

    @Test
    @DisplayName("이메일 제약이 아닌 저장 예외는 그대로 다시 던진다")
    void saveRethrowsNonEmailConstraintViolation() {
        DataIntegrityViolationException exception = new DataIntegrityViolationException("Column 'nickname' cannot be null");
        given(userJpaRepository.saveAndFlush(any(User.class))).willThrow(exception);

        assertThatThrownBy(() -> userStoreJpaAdapter.save(
                User.createUser("invalid@example.com", "encoded-password", "nickname")
        )).isSameAs(exception);
    }

    @Test
    @DisplayName("다른 컬럼 이름에 email이 포함돼도 중복 이메일 예외로 오인하지 않는다")
    void saveDoesNotTreatOtherEmailLikeColumnsAsDuplicateEmail() {
        DataIntegrityViolationException exception =
                new DataIntegrityViolationException("Duplicate entry 'value' for key 'users.seller_email'");
        given(userJpaRepository.saveAndFlush(any(User.class))).willThrow(exception);

        assertThatThrownBy(() -> userStoreJpaAdapter.save(
                User.createUser("invalid@example.com", "encoded-password", "nickname")
        )).isSameAs(exception);
    }

    @Test
    @DisplayName("명시적인 이메일 unique 제약 이름은 중복 이메일 예외로 변환한다")
    void saveMapsNamedEmailUniqueConstraint() {
        DataIntegrityViolationException exception =
                new DataIntegrityViolationException("Duplicate entry 'value' for key 'uk_users_email'");
        given(userJpaRepository.saveAndFlush(any(User.class))).willThrow(exception);

        assertThatThrownBy(() -> userStoreJpaAdapter.save(
                User.createUser("invalid@example.com", "encoded-password", "nickname")
        )).isInstanceOf(BusinessException.class)
                .extracting(throwable -> ((BusinessException) throwable).getErrorCode())
                .isEqualTo(ErrorCode.USER_EMAIL_ALREADY_EXISTS);
    }
}
