package com.fromvillage.auth.presentation.validation;

import jakarta.validation.ConstraintValidator;
import jakarta.validation.ConstraintValidatorContext;

public class PasswordConstraintValidator implements ConstraintValidator<ValidPassword, String> {

    private static final int MIN_LENGTH = 8;
    private static final int MAX_LENGTH = 20;
    private static final String LENGTH_MESSAGE = "비밀번호는 8자 이상 20자 이하로 입력해 주세요.";
    private static final String WHITESPACE_MESSAGE = "비밀번호에는 공백을 사용할 수 없습니다.";
    private static final String COMPOSITION_MESSAGE = "비밀번호는 영문 대문자, 영문 소문자, 숫자, 특수문자 중 3가지 이상을 포함해야 합니다.";

    @Override
    public boolean isValid(String value, ConstraintValidatorContext context) {
        if (value == null || value.isBlank()) {
            return true;
        }
        if (value.length() < MIN_LENGTH || value.length() > MAX_LENGTH) {
            return invalidate(context, LENGTH_MESSAGE);
        }
        if (containsWhitespace(value)) {
            return invalidate(context, WHITESPACE_MESSAGE);
        }
        if (countCharacterGroups(value) < 3) {
            return invalidate(context, COMPOSITION_MESSAGE);
        }
        return true;
    }

    private boolean containsWhitespace(String value) {
        for (int index = 0; index < value.length(); index++) {
            if (Character.isWhitespace(value.charAt(index))) {
                return true;
            }
        }
        return false;
    }

    private int countCharacterGroups(String value) {
        int count = 0;
        if (value.chars().anyMatch(Character::isUpperCase)) {
            count++;
        }
        if (value.chars().anyMatch(Character::isLowerCase)) {
            count++;
        }
        if (value.chars().anyMatch(Character::isDigit)) {
            count++;
        }
        if (value.chars().anyMatch(character -> isSpecialCharacter((char) character))) {
            count++;
        }
        return count;
    }

    private boolean isSpecialCharacter(char character) {
        return !Character.isLetterOrDigit(character) && !Character.isWhitespace(character);
    }

    private boolean invalidate(ConstraintValidatorContext context, String message) {
        context.disableDefaultConstraintViolation();
        context.buildConstraintViolationWithTemplate(message)
                .addConstraintViolation();
        return false;
    }
}
