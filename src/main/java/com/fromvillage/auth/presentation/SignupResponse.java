package com.fromvillage.auth.presentation;

import com.fromvillage.auth.application.SignupResult;

public record SignupResponse(
        String email,
        String nickname,
        String role
) {

    static SignupResponse from(SignupResult result) {
        return new SignupResponse(result.email(), result.nickname(), result.role());
    }
}
