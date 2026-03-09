package com.fromvillage.user.infrastructure;

import com.fromvillage.common.exception.BusinessException;
import com.fromvillage.common.exception.ErrorCode;
import com.fromvillage.user.domain.User;
import com.fromvillage.user.domain.UserStore;
import lombok.RequiredArgsConstructor;
import org.hibernate.exception.ConstraintViolationException;
import org.springframework.dao.DataIntegrityViolationException;
import org.springframework.stereotype.Component;

import java.util.Optional;
import java.util.regex.Pattern;

@Component
@RequiredArgsConstructor
public class UserStoreJpaAdapter implements UserStore {

    private static final String EMAIL_CONSTRAINT_NAME = "users.email";
    private static final String EMAIL_UNIQUE_CONSTRAINT_NAME = "uk_users_email";
    private static final Pattern EMAIL_KEY_PATTERN =
            Pattern.compile("for key ['`\"]?(users\\.)?email['`\"]?");

    private final UserJpaRepository userJpaRepository;

    @Override
    public boolean existsByEmail(String email) {
        return userJpaRepository.existsByEmail(email);
    }

    @Override
    public Optional<User> findByEmail(String email) {
        return userJpaRepository.findByEmail(email);
    }

    @Override
    public Optional<User> findById(Long id) {
        return userJpaRepository.findById(id);
    }

    @Override
    public User save(User user) {
        try {
            return userJpaRepository.saveAndFlush(user);
        } catch (DataIntegrityViolationException exception) {
            if (isEmailConstraintViolation(exception)) {
                throw new BusinessException(ErrorCode.USER_EMAIL_ALREADY_EXISTS);
            }
            throw exception;
        }
    }

    private boolean isEmailConstraintViolation(DataIntegrityViolationException exception) {
        Throwable cause = exception;
        while (cause != null) {
            if (cause instanceof ConstraintViolationException constraintViolationException) {
                String constraintName = constraintViolationException.getConstraintName();
                if (constraintName != null && isEmailConstraintName(constraintName.toLowerCase())) {
                    return true;
                }
            }
            cause = cause.getCause();
        }

        String message = exception.getMostSpecificCause() == null
                ? exception.getMessage()
                : exception.getMostSpecificCause().getMessage();

        if (message == null) {
            return false;
        }

        String normalizedMessage = message.toLowerCase();
        return isEmailConstraintName(normalizedMessage)
                || EMAIL_KEY_PATTERN.matcher(normalizedMessage).find();
    }

    private boolean isEmailConstraintName(String value) {
        return value.contains(EMAIL_CONSTRAINT_NAME)
                || value.contains(EMAIL_UNIQUE_CONSTRAINT_NAME);
    }
}
