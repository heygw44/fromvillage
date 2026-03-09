package com.fromvillage.auth.presentation;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.fromvillage.common.response.ApiResponse;
import com.fromvillage.support.TestContainersConfig;
import com.fromvillage.user.domain.User;
import com.fromvillage.user.infrastructure.UserJpaRepository;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.test.context.TestConfiguration;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Import;
import org.springframework.data.redis.core.StringRedisTemplate;
import org.springframework.http.MediaType;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.MvcResult;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import java.util.Map;
import java.util.Set;

import static org.assertj.core.api.Assertions.assertThat;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.cookie;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

@SpringBootTest
@AutoConfigureMockMvc
@ActiveProfiles("test")
@Import({
        TestContainersConfig.class,
        AuthSecurityIntegrationTest.SecurityProbeConfig.class
})
class AuthSecurityIntegrationTest {

    private static final String SESSION_NAMESPACE = "fromvillage:session:*";

    @Autowired
    private MockMvc mockMvc;

    @Autowired
    private ObjectMapper objectMapper;

    @Autowired
    private UserJpaRepository userRepository;

    @Autowired
    private PasswordEncoder passwordEncoder;

    @Autowired
    private StringRedisTemplate stringRedisTemplate;

    @BeforeEach
    void setUp() {
        userRepository.deleteAll();

        Set<String> keys = stringRedisTemplate.keys(SESSION_NAMESPACE);
        if (keys != null && !keys.isEmpty()) {
            stringRedisTemplate.delete(keys);
        }
    }

    @Test
    @DisplayName("CSRF 토큰을 조회할 수 있다")
    void csrfEndpointReturnsTokenPayload() throws Exception {
        mockMvc.perform(get("/api/v1/csrf"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.success").value(true))
                .andExpect(jsonPath("$.code").value("SUCCESS"))
                .andExpect(jsonPath("$.data.headerName").value("X-CSRF-TOKEN"))
                .andExpect(jsonPath("$.data.parameterName").value("_csrf"))
                .andExpect(jsonPath("$.data.token").isNotEmpty())
                .andExpect(cookie().exists("SESSION"));
    }

    @Test
    @DisplayName("로그인에 성공하면 세션 쿠키와 사용자 응답을 반환한다")
    void loginReturnsSessionCookieAndUserPayload() throws Exception {
        userRepository.saveAndFlush(User.createUser(
                "user@example.com",
                passwordEncoder.encode("Password12!"),
                "fromvillage"
        ));

        CsrfSession csrfSession = fetchCsrfSession();

        MvcResult result = mockMvc.perform(post("/api/v1/auth/login")
                        .cookie(csrfSession.sessionCookie())
                        .contentType(MediaType.APPLICATION_JSON)
                        .header(csrfSession.headerName(), csrfSession.token())
                        .content(objectMapper.writeValueAsString(Map.of(
                                "email", "user@example.com",
                                "password", "Password12!"
                        ))))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.success").value(true))
                .andExpect(jsonPath("$.data.email").value("user@example.com"))
                .andExpect(jsonPath("$.data.nickname").value("fromvillage"))
                .andExpect(jsonPath("$.data.role").value("USER"))
                .andReturn();

        var sessionCookie = result.getResponse().getCookie("SESSION");
        if (sessionCookie == null) {
            sessionCookie = csrfSession.sessionCookie();
        }

        mockMvc.perform(get("/test/security/protected").cookie(sessionCookie))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.data").value("ok"));
    }

    @Test
    @DisplayName("로그인 정보가 올바르지 않으면 AUTH_UNAUTHORIZED를 반환한다")
    void loginRejectsInvalidCredentials() throws Exception {
        userRepository.saveAndFlush(User.createUser(
                "user@example.com",
                passwordEncoder.encode("Password12!"),
                "fromvillage"
        ));

        CsrfSession csrfSession = fetchCsrfSession();

        mockMvc.perform(post("/api/v1/auth/login")
                        .cookie(csrfSession.sessionCookie())
                        .contentType(MediaType.APPLICATION_JSON)
                        .header(csrfSession.headerName(), csrfSession.token())
                        .content(objectMapper.writeValueAsString(Map.of(
                                "email", "user@example.com",
                                "password", "WrongPassword12!"
                        ))))
                .andExpect(status().isUnauthorized())
                .andExpect(jsonPath("$.code").value("AUTH_UNAUTHORIZED"))
                .andExpect(jsonPath("$.message").value("이메일 또는 비밀번호가 올바르지 않습니다."));
    }

    @Test
    @DisplayName("로그인 요청의 이메일이나 비밀번호가 비어 있으면 AUTH_UNAUTHORIZED를 반환한다")
    void loginRejectsBlankCredentials() throws Exception {
        CsrfSession csrfSession = fetchCsrfSession();

        mockMvc.perform(post("/api/v1/auth/login")
                        .cookie(csrfSession.sessionCookie())
                        .contentType(MediaType.APPLICATION_JSON)
                        .header(csrfSession.headerName(), csrfSession.token())
                        .content(objectMapper.writeValueAsString(Map.of(
                                "email", "",
                                "password", ""
                        ))))
                .andExpect(status().isUnauthorized())
                .andExpect(jsonPath("$.code").value("AUTH_UNAUTHORIZED"))
                .andExpect(jsonPath("$.message").value("이메일과 비밀번호는 필수입니다."));
    }

    @Test
    @DisplayName("세션 쿠키 없이 보호 경로에 접근하면 AUTH_UNAUTHORIZED를 반환한다")
    void protectedEndpointWithoutSessionReturnsUnauthorized() throws Exception {
        mockMvc.perform(get("/test/security/protected"))
                .andExpect(status().isUnauthorized())
                .andExpect(jsonPath("$.code").value("AUTH_UNAUTHORIZED"));
    }

    @Test
    @DisplayName("세션은 있지만 CSRF 토큰 없이 상태 변경 요청하면 AUTH_CSRF_INVALID를 반환한다")
    void protectedWriteWithoutCsrfReturnsCsrfInvalid() throws Exception {
        userRepository.saveAndFlush(User.createUser(
                "user@example.com",
                passwordEncoder.encode("Password12!"),
                "fromvillage"
        ));

        var loginSession = login("user@example.com", "Password12!");

        mockMvc.perform(post("/test/security/protected")
                        .cookie(loginSession))
                .andExpect(status().isForbidden())
                .andExpect(jsonPath("$.code").value("AUTH_CSRF_INVALID"));
    }

    @Test
    @DisplayName("USER가 ADMIN 전용 메서드에 접근하면 AUTH_FORBIDDEN을 반환한다")
    void adminMethodWithUserSessionReturnsForbidden() throws Exception {
        userRepository.saveAndFlush(User.createUser(
                "user@example.com",
                passwordEncoder.encode("Password12!"),
                "fromvillage"
        ));

        var loginSession = login("user@example.com", "Password12!");

        mockMvc.perform(get("/test/security/admin")
                        .cookie(loginSession))
                .andExpect(status().isForbidden())
                .andExpect(jsonPath("$.code").value("AUTH_FORBIDDEN"));
    }

    @Test
    @DisplayName("만료된 세션 쿠키로 보호 경로에 접근하면 AUTH_SESSION_EXPIRED를 반환한다")
    void protectedEndpointWithExpiredSessionReturnsSessionExpired() throws Exception {
        userRepository.saveAndFlush(User.createUser(
                "user@example.com",
                passwordEncoder.encode("Password12!"),
                "fromvillage"
        ));

        var loginSession = login("user@example.com", "Password12!");

        Set<String> keys = stringRedisTemplate.keys(SESSION_NAMESPACE);
        if (keys != null && !keys.isEmpty()) {
            stringRedisTemplate.delete(keys);
        }

        mockMvc.perform(get("/test/security/protected")
                        .cookie(loginSession))
                .andExpect(status().isUnauthorized())
                .andExpect(jsonPath("$.code").value("AUTH_SESSION_EXPIRED"));
    }

    @Test
    @DisplayName("로그아웃하면 기존 세션으로 더 이상 보호 경로에 접근할 수 없다")
    void logoutInvalidatesSession() throws Exception {
        userRepository.saveAndFlush(User.createUser(
                "user@example.com",
                passwordEncoder.encode("Password12!"),
                "fromvillage"
        ));

        var loginSession = login("user@example.com", "Password12!");
        var logoutCsrf = fetchCsrfSession(loginSession);

        mockMvc.perform(post("/api/v1/auth/logout")
                        .cookie(loginSession)
                        .contentType(MediaType.APPLICATION_JSON)
                        .header(logoutCsrf.headerName(), logoutCsrf.token()))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.success").value(true));

        mockMvc.perform(get("/test/security/protected")
                        .cookie(loginSession))
                .andExpect(status().isUnauthorized())
                .andExpect(jsonPath("$.code").value("AUTH_SESSION_EXPIRED"));
    }

    private jakarta.servlet.http.Cookie login(String email, String password) throws Exception {
        CsrfSession csrfSession = fetchCsrfSession();

        MvcResult result = mockMvc.perform(post("/api/v1/auth/login")
                        .cookie(csrfSession.sessionCookie())
                        .contentType(MediaType.APPLICATION_JSON)
                        .header(csrfSession.headerName(), csrfSession.token())
                        .content(objectMapper.writeValueAsString(Map.of(
                                "email", email,
                                "password", password
                        ))))
                .andExpect(status().isOk())
                .andReturn();

        var sessionCookie = result.getResponse().getCookie("SESSION");
        if (sessionCookie == null) {
            sessionCookie = csrfSession.sessionCookie();
        }
        return sessionCookie;
    }

    private CsrfSession fetchCsrfSession() throws Exception {
        MvcResult result = mockMvc.perform(get("/api/v1/csrf"))
                .andExpect(status().isOk())
                .andReturn();

        return toCsrfSession(result);
    }

    private CsrfSession fetchCsrfSession(jakarta.servlet.http.Cookie sessionCookie) throws Exception {
        MvcResult result = mockMvc.perform(get("/api/v1/csrf").cookie(sessionCookie))
                .andExpect(status().isOk())
                .andReturn();

        return toCsrfSession(result, sessionCookie);
    }

    private CsrfSession toCsrfSession(MvcResult result) throws Exception {
        return toCsrfSession(result, null);
    }

    private CsrfSession toCsrfSession(MvcResult result, jakarta.servlet.http.Cookie fallbackCookie) throws Exception {
        JsonNode root = objectMapper.readTree(result.getResponse().getContentAsString());
        var sessionCookie = result.getResponse().getCookie("SESSION");
        if (sessionCookie == null) {
            sessionCookie = fallbackCookie;
        }
        assertThat(sessionCookie).isNotNull();
        return new CsrfSession(
                sessionCookie,
                root.path("data").path("headerName").asText(),
                root.path("data").path("token").asText()
        );
    }

    private record CsrfSession(
            jakarta.servlet.http.Cookie sessionCookie,
            String headerName,
            String token
    ) {
    }

    @TestConfiguration(proxyBeanMethods = false)
    static class SecurityProbeConfig {

        @Bean
        SecurityProbeService securityProbeService() {
            return new SecurityProbeService();
        }

        @Bean
        SecurityProbeController securityProbeController(SecurityProbeService securityProbeService) {
            return new SecurityProbeController(securityProbeService);
        }
    }

    @RestController
    @RequestMapping("/test/security")
    static class SecurityProbeController {

        private final SecurityProbeService securityProbeService;

        SecurityProbeController(SecurityProbeService securityProbeService) {
            this.securityProbeService = securityProbeService;
        }

        @GetMapping("/protected")
        ApiResponse<String> protectedEndpoint() {
            return ApiResponse.success("ok");
        }

        @PostMapping("/protected")
        ApiResponse<String> protectedWrite(@RequestBody(required = false) Map<String, Object> body) {
            return ApiResponse.success("ok");
        }

        @GetMapping("/admin")
        ApiResponse<String> adminOnly() {
            return ApiResponse.success(securityProbeService.adminOnly());
        }
    }

    static class SecurityProbeService {

        @PreAuthorize("hasRole('ADMIN')")
        String adminOnly() {
            return "admin";
        }
    }
}
