package com.fromvillage.common.security;

import com.fromvillage.common.exception.ErrorCode;
import jakarta.servlet.ServletException;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import lombok.RequiredArgsConstructor;
import org.springframework.security.authentication.AuthenticationServiceException;
import org.springframework.security.core.AuthenticationException;
import org.springframework.security.web.authentication.AuthenticationFailureHandler;
import org.springframework.stereotype.Component;

import java.io.IOException;

@Component
@RequiredArgsConstructor
public class JsonLoginFailureHandler implements AuthenticationFailureHandler {

    private static final String INVALID_CREDENTIALS_MESSAGE = "이메일 또는 비밀번호가 올바르지 않습니다.";

    private final SecurityResponseWriter responseWriter;

    @Override
    public void onAuthenticationFailure(
            HttpServletRequest request,
            HttpServletResponse response,
            AuthenticationException exception
    ) throws IOException, ServletException {
        if (exception instanceof AuthenticationServiceException) {
            responseWriter.writeError(response, ErrorCode.AUTH_UNAUTHORIZED, exception.getMessage());
            return;
        }

        responseWriter.writeError(response, ErrorCode.AUTH_UNAUTHORIZED, INVALID_CREDENTIALS_MESSAGE);
    }
}
