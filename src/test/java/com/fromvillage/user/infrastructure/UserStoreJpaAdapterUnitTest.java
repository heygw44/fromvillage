package com.fromvillage.user.infrastructure;

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
}
