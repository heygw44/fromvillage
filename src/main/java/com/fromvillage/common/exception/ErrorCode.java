package com.fromvillage.common.exception;

import org.springframework.http.HttpStatus;

public enum ErrorCode {
    VALIDATION_ERROR(HttpStatus.BAD_REQUEST, "VALIDATION_ERROR", "입력한 내용을 다시 확인해 주세요."),
    AUTH_UNAUTHORIZED(HttpStatus.UNAUTHORIZED, "AUTH_UNAUTHORIZED", "로그인이 필요합니다."),
    AUTH_SESSION_EXPIRED(HttpStatus.UNAUTHORIZED, "AUTH_SESSION_EXPIRED", "로그인이 만료되었습니다. 다시 로그인해 주세요."),
    AUTH_CSRF_INVALID(HttpStatus.FORBIDDEN, "AUTH_CSRF_INVALID", "요청을 다시 시도해 주세요."),
    AUTH_FORBIDDEN(HttpStatus.FORBIDDEN, "AUTH_FORBIDDEN", "접근 권한이 없습니다."),
    AUTH_LOGIN_TEMPORARILY_LOCKED(HttpStatus.UNAUTHORIZED, "AUTH_LOGIN_TEMPORARILY_LOCKED", "로그인 시도가 잠시 제한되었습니다. 잠시 후 다시 시도해 주세요."),
    USER_NOT_FOUND(HttpStatus.NOT_FOUND, "USER_NOT_FOUND", "사용자를 찾을 수 없습니다."),
    USER_EMAIL_ALREADY_EXISTS(HttpStatus.CONFLICT, "USER_EMAIL_ALREADY_EXISTS", "이미 사용 중인 이메일입니다."),
    USER_ALREADY_SELLER(HttpStatus.CONFLICT, "USER_ALREADY_SELLER", "이미 판매자 권한이 부여된 계정입니다."),
    SELLER_APPROVAL_NOT_ALLOWED(HttpStatus.CONFLICT, "SELLER_APPROVAL_NOT_ALLOWED", "판매자 권한은 일반 회원에게만 부여할 수 있습니다."),
    PRODUCT_NOT_FOUND(HttpStatus.NOT_FOUND, "PRODUCT_NOT_FOUND", "상품을 찾을 수 없습니다."),
    PRODUCT_SELLER_ROLE_REQUIRED(HttpStatus.BAD_REQUEST, "PRODUCT_SELLER_ROLE_REQUIRED", "상품은 판매자 계정으로만 등록할 수 있습니다."),
    PRODUCT_PRICE_INVALID(HttpStatus.BAD_REQUEST, "PRODUCT_PRICE_INVALID", "상품 가격은 0보다 커야 합니다."),
    PRODUCT_STOCK_QUANTITY_INVALID(HttpStatus.BAD_REQUEST, "PRODUCT_STOCK_QUANTITY_INVALID", "상품 재고는 0 이상이어야 합니다."),
    PRODUCT_STOCK_INSUFFICIENT(HttpStatus.CONFLICT, "PRODUCT_STOCK_INSUFFICIENT", "상품 재고가 부족합니다."),
    PRODUCT_STOCK_QUANTITY_OVERFLOW(HttpStatus.BAD_REQUEST, "PRODUCT_STOCK_QUANTITY_OVERFLOW", "상품 재고 범위를 초과했습니다."),
    PRODUCT_IMAGE_URL_INVALID(HttpStatus.BAD_REQUEST, "PRODUCT_IMAGE_URL_INVALID", "상품 이미지는 https 주소만 사용할 수 있습니다."),
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
