package com.fromvillage.auth.presentation;

import com.fromvillage.auth.application.SignupResult;
import com.fromvillage.user.domain.UserRole;

public record SignupResponse(
        String email,
        String nickname,
        UserRole role
) {

    static SignupResponse from(SignupResult result) {
        return new SignupResponse(result.email(), result.nickname(), result.role());
    }
}
