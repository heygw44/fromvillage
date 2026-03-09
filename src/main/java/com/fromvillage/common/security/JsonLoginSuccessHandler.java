package com.fromvillage.common.security;

import com.fromvillage.common.exception.ErrorCode;
import jakarta.servlet.ServletException;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.security.core.Authentication;
import org.springframework.security.web.authentication.AuthenticationSuccessHandler;
import org.springframework.stereotype.Component;

import java.io.IOException;

@Slf4j
@Component
@RequiredArgsConstructor
public class JsonLoginSuccessHandler implements AuthenticationSuccessHandler {

    private final SecurityResponseWriter responseWriter;

    @Override
    public void onAuthenticationSuccess(
            HttpServletRequest request,
            HttpServletResponse response,
            Authentication authentication
    ) throws IOException, ServletException {
        Object principal = authentication.getPrincipal();
        if (!(principal instanceof AuthenticatedUser user)) {
            log.warn("Unexpected authentication principal type: {}", principal == null ? "null" : principal.getClass().getName());
            responseWriter.writeError(response, ErrorCode.COMMON_INTERNAL_ERROR);
            return;
        }

        responseWriter.writeSuccess(response, LoginSuccessResponse.from(user));
    }
}
