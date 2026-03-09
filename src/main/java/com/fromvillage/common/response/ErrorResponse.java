package com.fromvillage.common.response;

import com.fromvillage.common.exception.ErrorCode;

import java.util.List;

public record ErrorResponse(
        boolean success,
        String code,
        String message,
        Void data,
        List<ValidationErrorData> errors
) {

    public static ErrorResponse of(ErrorCode errorCode) {
        return of(errorCode, List.of());
    }

    public static ErrorResponse of(ErrorCode errorCode, String message) {
        return new ErrorResponse(
                false,
                errorCode.getCode(),
                message,
                null,
                List.of()
        );
    }

    public static ErrorResponse of(ErrorCode errorCode, List<ValidationErrorData> errors) {
        return new ErrorResponse(
                false,
                errorCode.getCode(),
                errorCode.getMessage(),
                null,
                List.copyOf(errors == null ? List.of() : errors)
        );
    }
}
