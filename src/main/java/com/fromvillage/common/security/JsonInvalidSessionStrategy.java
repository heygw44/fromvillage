package com.fromvillage.common.security;

import com.fromvillage.common.exception.ErrorCode;
import jakarta.servlet.ServletException;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import lombok.RequiredArgsConstructor;
import org.springframework.security.web.session.InvalidSessionStrategy;
import org.springframework.stereotype.Component;

import java.io.IOException;

@Component
@RequiredArgsConstructor
public class JsonInvalidSessionStrategy implements InvalidSessionStrategy {

    private final SecurityResponseWriter responseWriter;

    @Override
    public void onInvalidSessionDetected(HttpServletRequest request, HttpServletResponse response)
            throws IOException, ServletException {
        responseWriter.writeError(response, ErrorCode.AUTH_SESSION_EXPIRED);
    }
}
