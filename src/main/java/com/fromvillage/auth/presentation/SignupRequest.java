package com.fromvillage.auth.presentation;

import com.fromvillage.auth.application.SignupCommand;
import com.fromvillage.auth.presentation.validation.ValidPassword;
import jakarta.validation.constraints.Email;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;

public record SignupRequest(
        @NotBlank(message = "이메일이 입력되지 않았습니다.")
        @Email(message = "이메일 형식이 올바르지 않습니다.")
        @Size(max = 320, message = "이메일은 320자 이하로 입력해 주세요.")
        String email,

        @NotBlank(message = "비밀번호가 입력되지 않았습니다.")
        @ValidPassword
        String password,

        @NotBlank(message = "닉네임이 입력되지 않았습니다.")
        @Size(max = 50, message = "닉네임은 50자 이하로 입력해 주세요.")
        String nickname
) {

    SignupCommand toCommand() {
        return new SignupCommand(email, password, nickname);
    }
}
