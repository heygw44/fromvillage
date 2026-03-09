package com.fromvillage.auth.presentation;

import com.fromvillage.common.security.AuthenticatedUser;

public record LoginResponse(
        String email,
        String nickname,
        String role
) {

    public static LoginResponse from(AuthenticatedUser user) {
        return new LoginResponse(
                user.getUsername(),
                user.getNickname(),
                user.getRole()
        );
    }
}
