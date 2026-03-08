package com.fromvillage.auth.presentation;

import com.fromvillage.auth.application.SignupCommand;
import com.fromvillage.auth.application.SignupResult;
import com.fromvillage.auth.application.SignupService;
import com.fromvillage.common.response.ApiResponse;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpStatus;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;
import org.springframework.web.bind.annotation.ResponseStatus;

@RestController
@RequiredArgsConstructor
@RequestMapping("/api/v1/auth")
public class AuthController {

    private final SignupService signupService;

    @PostMapping("/signup")
    @ResponseStatus(HttpStatus.CREATED)
    public ApiResponse<SignupResponse> signup(@Valid @RequestBody SignupRequest request) {
        SignupResult result = signupService.signup(request.toCommand());
        return ApiResponse.success(SignupResponse.from(result));
    }
}
