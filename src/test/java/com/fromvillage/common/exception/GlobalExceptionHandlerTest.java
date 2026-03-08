package com.fromvillage.common.exception;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.fromvillage.common.response.ErrorResponse;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.http.HttpStatus;
import org.springframework.http.MediaType;
import org.springframework.http.converter.json.MappingJackson2HttpMessageConverter;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.setup.MockMvcBuilders;
import org.springframework.validation.beanvalidation.LocalValidatorFactoryBean;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import static org.assertj.core.api.Assertions.assertThat;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

class GlobalExceptionHandlerTest {

    private final ObjectMapper objectMapper = new ObjectMapper();
    private final MockMvc mockMvc = createMockMvc();

    @Test
    @DisplayName("BusinessException은 문서화된 에러 응답으로 변환된다")
    void businessExceptionConvertedToErrorResponse() throws Exception {
        mockMvc.perform(get("/test/business"))
                .andExpect(status().isForbidden())
                .andExpect(jsonPath("$.success").value(false))
                .andExpect(jsonPath("$.code").value("AUTH_FORBIDDEN"))
                .andExpect(jsonPath("$.message").value("접근 권한이 없습니다."))
                .andExpect(jsonPath("$.errors").isArray());
    }

    @Test
    @DisplayName("읽을 수 없는 요청 바디는 VALIDATION_ERROR로 변환된다")
    void unreadableBodyConvertedToValidationError() throws Exception {
        mockMvc.perform(post("/test/body")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{\"name\":"))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.success").value(false))
                .andExpect(jsonPath("$.code").value("VALIDATION_ERROR"))
                .andExpect(jsonPath("$.message").value("입력한 내용을 다시 확인해 주세요."));
    }

    @Test
    @DisplayName("예상하지 못한 예외는 COMMON_INTERNAL_ERROR로 변환된다")
    void unexpectedExceptionConvertedToInternalError() throws Exception {
        String responseBody = mockMvc.perform(get("/test/unexpected"))
                .andExpect(status().isInternalServerError())
                .andExpect(jsonPath("$.success").value(false))
                .andExpect(jsonPath("$.code").value("COMMON_INTERNAL_ERROR"))
                .andReturn()
                .getResponse()
                .getContentAsString();

        ErrorResponse response = objectMapper.readValue(responseBody, ErrorResponse.class);
        assertThat(response.message()).isEqualTo("일시적인 오류가 발생했습니다. 잠시 후 다시 시도해 주세요.");
        assertThat(response.data()).isNull();
        assertThat(response.errors()).isEmpty();
    }

    private MockMvc createMockMvc() {
        LocalValidatorFactoryBean validator = new LocalValidatorFactoryBean();
        validator.afterPropertiesSet();

        return MockMvcBuilders.standaloneSetup(new TestController())
                .setControllerAdvice(new GlobalExceptionHandler())
                .setMessageConverters(new MappingJackson2HttpMessageConverter(objectMapper))
                .setValidator(validator)
                .build();
    }

    @RestController
    @RequestMapping("/test")
    static class TestController {

        @GetMapping("/business")
        String businessException() {
            throw new BusinessException(ErrorCode.AUTH_FORBIDDEN);
        }

        @GetMapping("/unexpected")
        String unexpectedException() {
            throw new IllegalStateException("boom");
        }

        @PostMapping("/body")
        String unreadableBody(@RequestBody TestRequest request) {
            return request.name();
        }
    }

    record TestRequest(String name) {
    }
}
