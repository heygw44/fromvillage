package com.fromvillage.common.security;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.fromvillage.auth.application.LoginFailurePolicyService;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import jakarta.validation.ConstraintViolation;
import jakarta.validation.Validator;
import org.springframework.http.HttpMethod;
import org.springframework.http.MediaType;
import org.springframework.security.authentication.AuthenticationServiceException;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.AuthenticationException;
import org.springframework.security.web.authentication.UsernamePasswordAuthenticationFilter;

import java.io.IOException;

public class JsonLoginAuthenticationFilter extends UsernamePasswordAuthenticationFilter {

    public static final String LOGIN_EMAIL_ATTRIBUTE = JsonLoginAuthenticationFilter.class.getName() + ".loginEmail";
    public static final String INVALID_LOGIN_REQUEST_MESSAGE = "로그인 요청이 올바르지 않습니다.";
    public static final String EMPTY_LOGIN_REQUEST_MESSAGE = "로그인 요청 본문이 비어있습니다.";

    private final ObjectMapper objectMapper;
    private final Validator validator;
    private final LoginFailurePolicyService loginFailurePolicyService;

    public JsonLoginAuthenticationFilter(
            ObjectMapper objectMapper,
            Validator validator,
            LoginFailurePolicyService loginFailurePolicyService
    ) {
        this.objectMapper = objectMapper;
        this.validator = validator;
        this.loginFailurePolicyService = loginFailurePolicyService;
        setFilterProcessesUrl("/api/v1/auth/login");
    }

    @Override
    public Authentication attemptAuthentication(HttpServletRequest request, HttpServletResponse response)
            throws AuthenticationException {
        if (!HttpMethod.POST.matches(request.getMethod())) {
            throw new AuthenticationServiceException("지원하지 않는 로그인 메서드입니다.");
        }

        if (request.getContentType() == null || !request.getContentType().startsWith(MediaType.APPLICATION_JSON_VALUE)) {
            throw new AuthenticationServiceException("로그인 요청은 JSON이어야 합니다.");
        }

        LoginRequestPayload loginRequest = readLoginRequest(request);
        validate(loginRequest);
        request.setAttribute(LOGIN_EMAIL_ATTRIBUTE, loginRequest.email());

        if (loginFailurePolicyService.isLocked(loginRequest.email())) {
            throw new LoginTemporarilyLockedAuthenticationException();
        }

        UsernamePasswordAuthenticationToken authRequest =
                UsernamePasswordAuthenticationToken.unauthenticated(loginRequest.email(), loginRequest.password());
        setDetails(request, authRequest);
        return this.getAuthenticationManager().authenticate(authRequest);
    }

    private LoginRequestPayload readLoginRequest(HttpServletRequest request) {
        try {
            LoginRequestPayload loginRequest = objectMapper.readValue(request.getInputStream(), LoginRequestPayload.class);
            if (loginRequest == null) {
                throw new AuthenticationServiceException(EMPTY_LOGIN_REQUEST_MESSAGE);
            }
            return loginRequest;
        } catch (IOException exception) {
            throw new AuthenticationServiceException("로그인 요청 본문을 읽을 수 없습니다.", exception);
        }
    }

    private void validate(LoginRequestPayload loginRequest) {
        if (loginRequest == null) {
            throw new AuthenticationServiceException(EMPTY_LOGIN_REQUEST_MESSAGE);
        }

        for (ConstraintViolation<LoginRequestPayload> ignored : validator.validate(loginRequest)) {
            throw new AuthenticationServiceException(INVALID_LOGIN_REQUEST_MESSAGE);
        }
    }
}
