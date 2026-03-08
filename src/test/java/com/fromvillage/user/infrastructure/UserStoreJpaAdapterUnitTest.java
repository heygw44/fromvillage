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
