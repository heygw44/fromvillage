package com.fromvillage.common.exception;

import org.springframework.http.HttpStatus;

public enum ErrorCode {
    SUCCESS(HttpStatus.OK, "SUCCESS", "요청이 성공했습니다."),
    VALIDATION_ERROR(HttpStatus.BAD_REQUEST, "VALIDATION_ERROR", "입력한 내용을 다시 확인해 주세요."),
    AUTH_UNAUTHORIZED(HttpStatus.UNAUTHORIZED, "AUTH_UNAUTHORIZED", "로그인이 필요합니다."),
    AUTH_SESSION_EXPIRED(HttpStatus.UNAUTHORIZED, "AUTH_SESSION_EXPIRED", "로그인이 만료되었습니다. 다시 로그인해 주세요."),
    AUTH_CSRF_INVALID(HttpStatus.FORBIDDEN, "AUTH_CSRF_INVALID", "요청을 다시 시도해 주세요."),
    AUTH_FORBIDDEN(HttpStatus.FORBIDDEN, "AUTH_FORBIDDEN", "접근 권한이 없습니다."),
    AUTH_LOGIN_TEMPORARILY_LOCKED(HttpStatus.UNAUTHORIZED, "AUTH_LOGIN_TEMPORARILY_LOCKED", "로그인 시도가 잠시 제한되었습니다. 잠시 후 다시 시도해 주세요."),
    COMMON_INTERNAL_ERROR(HttpStatus.INTERNAL_SERVER_ERROR, "COMMON_INTERNAL_ERROR", "일시적인 오류가 발생했습니다. 잠시 후 다시 시도해 주세요.");

    private final HttpStatus status;
    private final String code;
    private final String message;

    ErrorCode(HttpStatus status, String code, String message) {
        this.status = status;
        this.code = code;
        this.message = message;
    }

    public HttpStatus getStatus() {
        return status;
    }

    public String getCode() {
        return code;
    }

    public String getMessage() {
        return message;
    }
}
