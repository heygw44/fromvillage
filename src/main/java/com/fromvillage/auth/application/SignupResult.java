package com.fromvillage.auth.application;

import com.fromvillage.user.domain.UserRole;

public record SignupResult(
        String email,
        String nickname,
        UserRole role
) {
}
