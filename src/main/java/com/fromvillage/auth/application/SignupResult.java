package com.fromvillage.auth.application;

public record SignupResult(
        String email,
        String nickname,
        String role
) {
}
