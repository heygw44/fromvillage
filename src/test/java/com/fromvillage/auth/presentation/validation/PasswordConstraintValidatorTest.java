package com.fromvillage.auth.presentation.validation;

import jakarta.validation.ConstraintViolation;
import jakarta.validation.Validation;
import jakarta.validation.Validator;
import jakarta.validation.ValidatorFactory;
import org.junit.jupiter.api.AfterAll;
import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

import java.util.Set;

import static org.assertj.core.api.Assertions.assertThat;

class PasswordConstraintValidatorTest {

    private static ValidatorFactory validatorFactory;
    private static Validator validator;

    @BeforeAll
    static void setUp() {
        validatorFactory = Validation.buildDefaultValidatorFactory();
        validator = validatorFactory.getValidator();
    }

    @AfterAll
    static void tearDown() {
        validatorFactory.close();
    }

    @Test
    @DisplayName("8자 미만 비밀번호는 길이 메시지로 거절한다")
    void rejectsPasswordShorterThanEightCharacters() {
        Set<ConstraintViolation<PasswordHolder>> violations = validator.validate(new PasswordHolder("Aa1!aaa"));

        assertThat(violations).hasSize(1);
        assertThat(violations.iterator().next().getMessage()).isEqualTo("비밀번호는 8자 이상 20자 이하로 입력해 주세요.");
    }

    @Test
    @DisplayName("공백이 포함된 비밀번호는 공백 메시지로 거절한다")
    void rejectsPasswordContainingWhitespace() {
        Set<ConstraintViolation<PasswordHolder>> violations = validator.validate(new PasswordHolder("Password 12!"));

        assertThat(violations).hasSize(1);
        assertThat(violations.iterator().next().getMessage()).isEqualTo("비밀번호에는 공백을 사용할 수 없습니다.");
    }

    @Test
    @DisplayName("조합이 부족한 비밀번호는 조합 메시지로 거절한다")
    void rejectsPasswordWithInsufficientCharacterGroups() {
        Set<ConstraintViolation<PasswordHolder>> violations = validator.validate(new PasswordHolder("password12"));

        assertThat(violations).hasSize(1);
        assertThat(violations.iterator().next().getMessage())
                .isEqualTo("비밀번호는 영문 대문자, 영문 소문자, 숫자, 특수문자 중 3가지 이상을 포함해야 합니다.");
    }

    @Test
    @DisplayName("정책을 만족하는 비밀번호는 통과한다")
    void acceptsValidPassword() {
        Set<ConstraintViolation<PasswordHolder>> violations = validator.validate(new PasswordHolder("Password12!"));

        assertThat(violations).isEmpty();
    }

    private record PasswordHolder(
            @ValidPassword String password
    ) {
    }
}
