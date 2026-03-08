package com.fromvillage.auth.application;

import com.fromvillage.common.exception.BusinessException;
import com.fromvillage.common.exception.ErrorCode;
import com.fromvillage.user.domain.User;
import com.fromvillage.user.infrastructure.UserRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.dao.DataIntegrityViolationException;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
@RequiredArgsConstructor
public class SignupService {

    private final UserRepository userRepository;
    private final PasswordEncoder passwordEncoder;
    private static final String EMAIL_COLUMN_NAME = "email";
    private static final String EMAIL_CONSTRAINT_NAME = "users.email";

    @Transactional
    public SignupResult signup(SignupCommand command) {
        if (userRepository.existsByEmail(command.email())) {
            throw new BusinessException(ErrorCode.USER_EMAIL_ALREADY_EXISTS);
        }

        User user = User.createUser(
                command.email(),
                passwordEncoder.encode(command.password()),
                command.nickname()
        );

        try {
            User savedUser = userRepository.saveAndFlush(user);
            return new SignupResult(savedUser.getEmail(), savedUser.getNickname(), savedUser.getRole().name());
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
        return normalizedMessage.contains(EMAIL_COLUMN_NAME) || normalizedMessage.contains(EMAIL_CONSTRAINT_NAME);
    }
}
