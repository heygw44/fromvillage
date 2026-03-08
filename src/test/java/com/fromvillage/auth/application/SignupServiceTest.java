package com.fromvillage.auth.application;

import com.fromvillage.common.exception.BusinessException;
import com.fromvillage.common.exception.ErrorCode;
import com.fromvillage.user.domain.User;
import com.fromvillage.user.domain.UserRole;
import com.fromvillage.user.infrastructure.UserRepository;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.dao.DataIntegrityViolationException;
import org.springframework.security.crypto.password.PasswordEncoder;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.BDDMockito.given;
import static org.mockito.Mockito.verify;

@ExtendWith(MockitoExtension.class)
class SignupServiceTest {

    @Mock
    private UserRepository userRepository;

    @Mock
    private PasswordEncoder passwordEncoder;

    @InjectMocks
    private SignupService signupService;

    @Test
    @DisplayName("회원가입 시 비밀번호를 해시 저장하고 USER 역할 결과를 반환한다")
    void signupEncodesPasswordAndReturnsUserRole() {
        SignupCommand command = new SignupCommand("user@example.com", "Password12!", "fromvillage");
        given(userRepository.existsByEmail("user@example.com")).willReturn(false);
        given(passwordEncoder.encode("Password12!")).willReturn("encoded-password");
        given(userRepository.saveAndFlush(any(User.class)))
                .willAnswer(invocation -> invocation.getArgument(0));

        SignupResult result = signupService.signup(command);

        ArgumentCaptor<User> userCaptor = ArgumentCaptor.forClass(User.class);
        verify(userRepository).saveAndFlush(userCaptor.capture());

        User savedUser = userCaptor.getValue();
        assertThat(savedUser.getEmail()).isEqualTo("user@example.com");
        assertThat(savedUser.getNickname()).isEqualTo("fromvillage");
        assertThat(savedUser.getRole()).isEqualTo(UserRole.USER);
        assertThat(savedUser.getPasswordHash()).isEqualTo("encoded-password");
        assertThat(result.email()).isEqualTo("user@example.com");
        assertThat(result.nickname()).isEqualTo("fromvillage");
        assertThat(result.role()).isEqualTo(UserRole.USER);
    }

    @Test
    @DisplayName("이미 사용 중인 이메일이면 회원가입을 거절한다")
    void signupRejectsDuplicateEmail() {
        given(userRepository.existsByEmail("duplicate@example.com")).willReturn(true);

        assertThatThrownBy(() -> signupService.signup(
                new SignupCommand("duplicate@example.com", "Password12!", "fromvillage")
        )).isInstanceOf(BusinessException.class)
                .extracting(exception -> ((BusinessException) exception).getErrorCode())
                .isEqualTo(ErrorCode.USER_EMAIL_ALREADY_EXISTS);
    }

    @Test
    @DisplayName("저장 시점에 중복 이메일 제약이 발생해도 같은 에러 코드로 변환한다")
    void signupMapsDuplicateConstraintOnSave() {
        given(userRepository.existsByEmail("duplicate@example.com")).willReturn(false);
        given(passwordEncoder.encode("Password12!")).willReturn("encoded-password");
        given(userRepository.saveAndFlush(any(User.class)))
                .willThrow(new DataIntegrityViolationException("duplicate"));

        assertThatThrownBy(() -> signupService.signup(
                new SignupCommand("duplicate@example.com", "Password12!", "fromvillage")
        )).isInstanceOf(BusinessException.class)
                .extracting(exception -> ((BusinessException) exception).getErrorCode())
                .isEqualTo(ErrorCode.USER_EMAIL_ALREADY_EXISTS);
    }
}
