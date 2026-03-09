package com.fromvillage.common.security;

public record LoginSuccessResponse(
        String email,
        String nickname,
        String role
) {

    public static LoginSuccessResponse from(AuthenticatedUser user) {
        return new LoginSuccessResponse(
                user.getUsername(),
                user.getNickname(),
                user.getRole()
        );
    }
}
