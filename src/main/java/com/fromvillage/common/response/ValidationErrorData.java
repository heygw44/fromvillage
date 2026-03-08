package com.fromvillage.common.response;

public record ValidationErrorData(
        String field,
        String reason
) {
}
