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
            return new SignupResult(savedUser.getEmail(), savedUser.getNickname(), savedUser.getRole());
        } catch (DataIntegrityViolationException exception) {
            throw new BusinessException(ErrorCode.USER_EMAIL_ALREADY_EXISTS);
        }
    }
}
