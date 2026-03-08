package com.fromvillage.common.response;

import com.fasterxml.jackson.databind.ObjectMapper;
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
        assertThat(response.code()).isEqualTo("SUCCESS");
        assertThat(response.message()).isEqualTo("요청이 성공했습니다.");
        assertThat(json).contains("\"success\":true");
        assertThat(json).contains("\"code\":\"SUCCESS\"");
        assertThat(json).contains("\"message\":\"요청이 성공했습니다.\"");
        assertThat(json).contains("\"name\":\"fromvillage\"");
    }

    @Test
    @DisplayName("data가 null이어도 성공 응답 메타데이터는 유지된다")
    void successResponseWithNullDataSerializesCorrectly() throws Exception {
        ApiResponse<Void> response = ApiResponse.success(null);

        String json = objectMapper.writeValueAsString(response);

        assertThat(response.success()).isTrue();
        assertThat(response.code()).isEqualTo("SUCCESS");
        assertThat(response.message()).isEqualTo("요청이 성공했습니다.");
        assertThat(response.data()).isNull();
        assertThat(json).contains("\"data\":null");
    }

    private record TestPayload(String name) {
    }
}
