package com.fromvillage.common.security;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.fromvillage.common.exception.ErrorCode;
import com.fromvillage.common.response.ApiResponse;
import com.fromvillage.common.response.ErrorResponse;
import jakarta.servlet.http.HttpServletResponse;
import lombok.RequiredArgsConstructor;
import org.springframework.http.MediaType;
import org.springframework.stereotype.Component;

import java.io.IOException;

@Component
@RequiredArgsConstructor
public class SecurityResponseWriter {

    private final ObjectMapper objectMapper;

    public void writeSuccess(HttpServletResponse response, Object body) throws IOException {
        response.setStatus(HttpServletResponse.SC_OK);
        response.setContentType(MediaType.APPLICATION_JSON_VALUE);
        objectMapper.writeValue(response.getOutputStream(), ApiResponse.success(body));
    }

    public void writeError(HttpServletResponse response, ErrorCode errorCode) throws IOException {
        writeError(response, errorCode, errorCode.getMessage());
    }

    public void writeError(HttpServletResponse response, ErrorCode errorCode, String message) throws IOException {
        response.setStatus(errorCode.getStatus().value());
        response.setContentType(MediaType.APPLICATION_JSON_VALUE);
        objectMapper.writeValue(response.getOutputStream(), ErrorResponse.of(errorCode, message));
    }
}
