package com.fromvillage.user.infrastructure;

import com.fromvillage.common.exception.BusinessException;
import com.fromvillage.common.exception.ErrorCode;
import com.fromvillage.user.domain.User;
import com.fromvillage.user.domain.UserStore;
import lombok.RequiredArgsConstructor;
import org.springframework.dao.DataIntegrityViolationException;
import org.springframework.stereotype.Component;

@Component
@RequiredArgsConstructor
public class UserStoreJpaAdapter implements UserStore {

    private static final String EMAIL_COLUMN_NAME = "email";
    private static final String EMAIL_CONSTRAINT_NAME = "users.email";
    private static final String DUPLICATE_ENTRY_TOKEN = "duplicate entry";
    private static final String USER_TABLE_TOKEN = "users.";

    private final UserJpaRepository userJpaRepository;

    @Override
    public boolean existsByEmail(String email) {
        return userJpaRepository.existsByEmail(email);
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
        String message = exception.getMostSpecificCause() == null
                ? exception.getMessage()
                : exception.getMostSpecificCause().getMessage();

        if (message == null) {
            return false;
        }

        String normalizedMessage = message.toLowerCase();
        return normalizedMessage.contains(EMAIL_CONSTRAINT_NAME)
                || normalizedMessage.contains(EMAIL_COLUMN_NAME)
                || (normalizedMessage.contains(DUPLICATE_ENTRY_TOKEN) && normalizedMessage.contains(USER_TABLE_TOKEN));
    }
}
