package com.fromvillage.auth.presentation;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.fromvillage.support.TestContainersConfig;
import com.fromvillage.user.domain.User;
import com.fromvillage.user.domain.UserRole;
import com.fromvillage.user.infrastructure.UserRepository;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.context.annotation.Import;
import org.springframework.http.MediaType;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.test.web.servlet.MockMvc;

import java.util.Map;

import static org.assertj.core.api.Assertions.assertThat;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

@SpringBootTest
@AutoConfigureMockMvc(addFilters = false)
@ActiveProfiles("test")
@Import(TestContainersConfig.class)
class AuthSignupIntegrationTest {

    @Autowired
    private MockMvc mockMvc;

    @Autowired
    private ObjectMapper objectMapper;

    @Autowired
    private UserRepository userRepository;

    @Autowired(required = false)
    private PasswordEncoder passwordEncoder;

    @BeforeEach
    void setUp() {
        userRepository.deleteAll();
    }

    @Test
    @DisplayName("회원가입에 성공하면 USER 역할 응답을 반환하고 비밀번호를 해시 저장한다")
    void signupCreatesUserAndReturnsSuccessResponse() throws Exception {
        mockMvc.perform(post("/api/v1/auth/signup")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(Map.of(
                                "email", "user@example.com",
                                "password", "Password12!",
                                "nickname", "fromvillage"
                        ))))
                .andExpect(status().isCreated())
                .andExpect(jsonPath("$.success").value(true))
                .andExpect(jsonPath("$.code").value("SUCCESS"))
                .andExpect(jsonPath("$.data.email").value("user@example.com"))
                .andExpect(jsonPath("$.data.nickname").value("fromvillage"))
                .andExpect(jsonPath("$.data.role").value("USER"))
                .andExpect(jsonPath("$.data.id").doesNotExist())
                .andExpect(jsonPath("$.data.password").doesNotExist());

        User savedUser = userRepository.findByEmail("user@example.com").orElseThrow();

        assertThat(savedUser.getRole()).isEqualTo(UserRole.USER);
        assertThat(savedUser.getPasswordHash()).isNotEqualTo("Password12!");
        assertThat(passwordEncoder).isNotNull();
        assertThat(passwordEncoder.matches("Password12!", savedUser.getPasswordHash())).isTrue();
    }

    @Test
    @DisplayName("이미 사용 중인 이메일이면 409와 중복 이메일 에러를 반환한다")
    void signupRejectsDuplicateEmail() throws Exception {
        userRepository.saveAndFlush(User.createUser("duplicate@example.com", "encoded-password", "first"));

        mockMvc.perform(post("/api/v1/auth/signup")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(Map.of(
                                "email", "duplicate@example.com",
                                "password", "Password12!",
                                "nickname", "second"
                        ))))
                .andExpect(status().isConflict())
                .andExpect(jsonPath("$.success").value(false))
                .andExpect(jsonPath("$.code").value("USER_EMAIL_ALREADY_EXISTS"))
                .andExpect(jsonPath("$.message").value("이미 사용 중인 이메일입니다."));
    }

    @Test
    @DisplayName("비밀번호 길이가 8자 미만이면 400 검증 에러를 반환한다")
    void signupRejectsShortPassword() throws Exception {
        mockMvc.perform(post("/api/v1/auth/signup")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(Map.of(
                                "email", "user@example.com",
                                "password", "Aa1!aaa",
                                "nickname", "fromvillage"
                        ))))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.success").value(false))
                .andExpect(jsonPath("$.code").value("VALIDATION_ERROR"))
                .andExpect(jsonPath("$.errors[0].field").value("password"))
                .andExpect(jsonPath("$.errors[0].reason").value("비밀번호는 8자 이상 20자 이하로 입력해 주세요."));
    }

    @Test
    @DisplayName("비밀번호에 공백이 포함되면 400 검증 에러를 반환한다")
    void signupRejectsPasswordWithWhitespace() throws Exception {
        mockMvc.perform(post("/api/v1/auth/signup")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(Map.of(
                                "email", "user@example.com",
                                "password", "Password 12!",
                                "nickname", "fromvillage"
                        ))))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.success").value(false))
                .andExpect(jsonPath("$.code").value("VALIDATION_ERROR"))
                .andExpect(jsonPath("$.errors[0].field").value("password"))
                .andExpect(jsonPath("$.errors[0].reason").value("비밀번호에는 공백을 사용할 수 없습니다."));
    }

    @Test
    @DisplayName("비밀번호 조합이 부족하면 400 검증 에러를 반환한다")
    void signupRejectsWeakPasswordComposition() throws Exception {
        mockMvc.perform(post("/api/v1/auth/signup")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(Map.of(
                                "email", "user@example.com",
                                "password", "password12",
                                "nickname", "fromvillage"
                        ))))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.success").value(false))
                .andExpect(jsonPath("$.code").value("VALIDATION_ERROR"))
                .andExpect(jsonPath("$.errors[0].field").value("password"))
                .andExpect(jsonPath("$.errors[0].reason").value("비밀번호는 영문 대문자, 영문 소문자, 숫자, 특수문자 중 3가지 이상을 포함해야 합니다."));
    }

    @Test
    @DisplayName("이메일 형식이 올바르지 않으면 400 검증 에러를 반환한다")
    void signupRejectsInvalidEmailFormat() throws Exception {
        mockMvc.perform(post("/api/v1/auth/signup")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(Map.of(
                                "email", "not-an-email",
                                "password", "Password12!",
                                "nickname", "fromvillage"
                        ))))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.success").value(false))
                .andExpect(jsonPath("$.code").value("VALIDATION_ERROR"))
                .andExpect(jsonPath("$.errors[0].field").value("email"))
                .andExpect(jsonPath("$.errors[0].reason").value("이메일 형식이 올바르지 않습니다."));
    }

    @Test
    @DisplayName("이메일이 320자를 초과하면 400 검증 에러를 반환한다")
    void signupRejectsTooLongEmail() throws Exception {
        String email = "a".repeat(64)
                + "@"
                + "b".repeat(63)
                + "."
                + "c".repeat(63)
                + "."
                + "d".repeat(63)
                + "."
                + "e".repeat(63)
                + "."
                + "f".repeat(58);

        mockMvc.perform(post("/api/v1/auth/signup")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(Map.of(
                                "email", email,
                                "password", "Password12!",
                                "nickname", "fromvillage"
                        ))))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.success").value(false))
                .andExpect(jsonPath("$.code").value("VALIDATION_ERROR"))
                .andExpect(jsonPath("$.errors[0].field").value("email"))
                .andExpect(jsonPath("$.errors[0].reason").value("이메일은 320자 이하로 입력해 주세요."));
    }

    @Test
    @DisplayName("닉네임이 비어 있으면 400 검증 에러를 반환한다")
    void signupRejectsBlankNickname() throws Exception {
        mockMvc.perform(post("/api/v1/auth/signup")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(Map.of(
                                "email", "user@example.com",
                                "password", "Password12!",
                                "nickname", " "
                        ))))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.success").value(false))
                .andExpect(jsonPath("$.code").value("VALIDATION_ERROR"))
                .andExpect(jsonPath("$.errors[0].field").value("nickname"))
                .andExpect(jsonPath("$.errors[0].reason").value("닉네임이 입력되지 않았습니다."));
    }

    @Test
    @DisplayName("닉네임이 50자를 초과하면 400 검증 에러를 반환한다")
    void signupRejectsTooLongNickname() throws Exception {
        mockMvc.perform(post("/api/v1/auth/signup")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(Map.of(
                                "email", "user@example.com",
                                "password", "Password12!",
                                "nickname", "a".repeat(51)
                        ))))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.success").value(false))
                .andExpect(jsonPath("$.code").value("VALIDATION_ERROR"))
                .andExpect(jsonPath("$.errors[0].field").value("nickname"))
                .andExpect(jsonPath("$.errors[0].reason").value("닉네임은 50자 이하로 입력해 주세요."));
    }
}
