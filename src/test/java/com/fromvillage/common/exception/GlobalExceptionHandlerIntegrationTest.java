package com.fromvillage.common.exception;

import jakarta.validation.Valid;
import jakarta.validation.constraints.Min;
import jakarta.validation.constraints.NotBlank;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc;
import org.springframework.boot.test.autoconfigure.web.servlet.WebMvcTest;
import org.springframework.context.annotation.Import;
import org.springframework.http.MediaType;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

@WebMvcTest(controllers = GlobalExceptionHandlerIntegrationTest.TestValidationController.class)
@AutoConfigureMockMvc(addFilters = false)
@Import({
        GlobalExceptionHandler.class,
        GlobalExceptionHandlerIntegrationTest.TestValidationController.class
})
class GlobalExceptionHandlerIntegrationTest {

    @Autowired
    private MockMvc mockMvc;

    @Test
    @DisplayName("RequestBody 검증 실패는 MethodArgumentNotValidException 경로로 응답한다")
    void requestBodyValidationHandledAsMethodArgumentNotValidException() throws Exception {
        mockMvc.perform(post("/validation/body")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{\"email\":\"\"}"))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.code").value("VALIDATION_ERROR"))
                .andExpect(jsonPath("$.message").value("입력한 내용을 다시 확인해 주세요."))
                .andExpect(jsonPath("$.errors[0].field").value("email"));
    }

    @Test
    @DisplayName("메서드 파라미터 검증 실패는 HandlerMethodValidationException 경로로 응답한다")
    void methodValidationHandledAsHandlerMethodValidationException() throws Exception {
        mockMvc.perform(get("/validation/param").param("age", "0"))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.code").value("VALIDATION_ERROR"))
                .andExpect(jsonPath("$.message").value("입력한 내용을 다시 확인해 주세요."))
                .andExpect(jsonPath("$.errors[0].field").value("age"));
    }

    @RestController
    @RequestMapping("/validation")
    public static class TestValidationController {

        @PostMapping("/body")
        String body(@Valid @RequestBody TestRequest request) {
            return request.email();
        }

        @GetMapping("/param")
        String param(@RequestParam @Min(1) int age) {
            return String.valueOf(age);
        }
    }

    public record TestRequest(@NotBlank(message = "이메일은 필수입니다.") String email) {
    }
}
