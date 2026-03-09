package com.fromvillage.auth.presentation;

public record LoginRequest(
        String email,
        String password
) {
}
