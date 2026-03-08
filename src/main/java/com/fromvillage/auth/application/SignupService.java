package com.fromvillage.auth.application;

import com.fromvillage.common.exception.BusinessException;
import com.fromvillage.common.exception.ErrorCode;
import com.fromvillage.user.domain.User;
import com.fromvillage.user.domain.UserStore;
import lombok.RequiredArgsConstructor;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
@RequiredArgsConstructor
public class SignupService {

    private final UserStore userStore;
    private final PasswordEncoder passwordEncoder;

    @Transactional
    public SignupResult signup(SignupCommand command) {
        if (userStore.existsByEmail(command.email())) {
            throw new BusinessException(ErrorCode.USER_EMAIL_ALREADY_EXISTS);
        }

        User user = User.createUser(
                command.email(),
                passwordEncoder.encode(command.password()),
                command.nickname()
        );

        User savedUser = userStore.save(user);
        return new SignupResult(savedUser.getEmail(), savedUser.getNickname(), savedUser.getRole().name());
    }
}
