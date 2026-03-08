package com.fromvillage.common.response;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.fromvillage.common.exception.ErrorCode;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

import static org.assertj.core.api.Assertions.assertThat;

class ApiResponseTest {

    private final ObjectMapper objectMapper = new ObjectMapper();

    @Test
    @DisplayName("성공 응답은 공통 계약 형식으로 직렬화된다")
    void successResponseSerializesWithCommonContract() throws Exception {
        ApiResponse<TestPayload> response = ApiResponse.success(new TestPayload("fromvillage"));

        String json = objectMapper.writeValueAsString(response);

        assertThat(response.success()).isTrue();
        assertThat(response.code()).isEqualTo(ErrorCode.SUCCESS.getCode());
        assertThat(response.message()).isEqualTo(ErrorCode.SUCCESS.getMessage());
        assertThat(json).contains("\"success\":true");
        assertThat(json).contains("\"code\":\"SUCCESS\"");
        assertThat(json).contains("\"message\":\"요청이 성공했습니다.\"");
        assertThat(json).contains("\"name\":\"fromvillage\"");
    }

    private record TestPayload(String name) {
    }
}
