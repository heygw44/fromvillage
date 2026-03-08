package com.fromvillage.auth.application;

public record SignupCommand(
        String email,
        String password,
        String nickname
) {
}
