package com.fromvillage.common.exception;

import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

class BusinessExceptionTest {

    @Test
    @DisplayName("BusinessException은 ErrorCode를 그대로 보존한다")
    void businessExceptionKeepsErrorCode() {
        BusinessException exception = new BusinessException(ErrorCode.AUTH_FORBIDDEN);

        assertThat(exception.getErrorCode()).isEqualTo(ErrorCode.AUTH_FORBIDDEN);
        assertThat(exception.getMessage()).isEqualTo("접근 권한이 없습니다.");
    }

    @Test
    @DisplayName("BusinessException은 null ErrorCode를 허용하지 않는다")
    void businessExceptionRequiresErrorCode() {
        assertThatThrownBy(() -> new BusinessException(null))
                .isInstanceOf(IllegalArgumentException.class)
                .hasMessage("에러 코드는 필수입니다.");
    }
}
