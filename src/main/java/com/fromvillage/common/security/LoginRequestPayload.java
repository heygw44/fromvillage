package com.fromvillage.common.security;

import jakarta.validation.constraints.NotBlank;

public record LoginRequestPayload(
        @NotBlank(message = "이메일과 비밀번호는 필수입니다.") String email,
        @NotBlank(message = "이메일과 비밀번호는 필수입니다.") String password
) {
}
