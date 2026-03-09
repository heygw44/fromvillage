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
import org.springframework.context.annotation.Primary;
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
import java.time.Clock;
import java.time.Duration;
import java.time.Instant;
import java.time.LocalDateTime;
import java.time.ZoneId;

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
    private static final String LOGIN_FAILURE_NAMESPACE = "fromvillage:auth:login-failure:*";

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

    @Autowired
    private MutableClock clock;

    @BeforeEach
    void setUp() {
        userRepository.deleteAll();
        clock.reset(Instant.parse("2026-03-09T00:00:00Z"));

        Set<String> keys = stringRedisTemplate.keys(SESSION_NAMESPACE);
        if (keys != null && !keys.isEmpty()) {
            stringRedisTemplate.delete(keys);
        }

        Set<String> loginFailureKeys = stringRedisTemplate.keys(LOGIN_FAILURE_NAMESPACE);
        if (loginFailureKeys != null && !loginFailureKeys.isEmpty()) {
            stringRedisTemplate.delete(loginFailureKeys);
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
    @DisplayName("로그인에 성공하면 기존 익명 세션과 다른 세션 ID가 발급된다")
    void loginChangesSessionIdAfterAuthentication() throws Exception {
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
                .andReturn();

        var loginSessionCookie = result.getResponse().getCookie("SESSION");

        assertThat(loginSessionCookie).isNotNull();
        assertThat(loginSessionCookie.getValue()).isNotEqualTo(csrfSession.sessionCookie().getValue());
        assertThat(loginSessionCookie.isHttpOnly()).isTrue();
        assertThat(loginSessionCookie.getSecure()).isTrue();
        assertThat(result.getResponse().getHeaders("Set-Cookie"))
                .anySatisfy(cookieHeader -> assertThat(cookieHeader).contains("SameSite=Lax"));
    }

    @Test
    @DisplayName("같은 계정으로 새 로그인하면 기존 세션은 만료된다")
    void secondLoginExpiresPreviousSession() throws Exception {
        userRepository.saveAndFlush(User.createUser(
                "user@example.com",
                passwordEncoder.encode("Password12!"),
                "fromvillage"
        ));

        var firstSession = login("user@example.com", "Password12!");
        var secondSession = login("user@example.com", "Password12!");

        mockMvc.perform(get("/test/security/protected").cookie(firstSession))
                .andExpect(status().isUnauthorized())
                .andExpect(jsonPath("$.code").value("AUTH_SESSION_EXPIRED"));

        mockMvc.perform(get("/test/security/protected").cookie(secondSession))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.data").value("ok"));
    }

    @Test
    @DisplayName("세션 쿠키는 기본 보안 속성을 포함한다")
    void sessionCookieUsesDefaultSecurityAttributes() throws Exception {
        MvcResult result = mockMvc.perform(get("/api/v1/csrf"))
                .andExpect(status().isOk())
                .andReturn();

        var sessionCookie = result.getResponse().getCookie("SESSION");

        assertThat(sessionCookie).isNotNull();
        assertThat(sessionCookie.isHttpOnly()).isTrue();
        assertThat(sessionCookie.getSecure()).isTrue();
        assertThat(result.getResponse().getHeaders("Set-Cookie"))
                .anySatisfy(cookieHeader -> assertThat(cookieHeader).contains("SameSite=Lax"));
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
    @DisplayName("잘못된 비밀번호가 5회 연속 누적되면 5번째 요청부터 AUTH_LOGIN_TEMPORARILY_LOCKED를 반환한다")
    void loginLocksImmediatelyOnFifthFailure() throws Exception {
        userRepository.saveAndFlush(User.createUser(
                "user@example.com",
                passwordEncoder.encode("Password12!"),
                "fromvillage"
        ));

        for (int attempt = 1; attempt <= 4; attempt++) {
            expectInvalidCredentials("user@example.com", "WrongPassword12!", "AUTH_UNAUTHORIZED");
        }

        expectInvalidCredentials("user@example.com", "WrongPassword12!", "AUTH_LOGIN_TEMPORARILY_LOCKED");
    }

    @Test
    @DisplayName("잠금된 계정은 올바른 비밀번호로도 AUTH_LOGIN_TEMPORARILY_LOCKED를 반환한다")
    void lockedAccountRejectsCorrectPasswordUntilLockExpires() throws Exception {
        userRepository.saveAndFlush(User.createUser(
                "user@example.com",
                passwordEncoder.encode("Password12!"),
                "fromvillage"
        ));

        for (int attempt = 1; attempt <= 5; attempt++) {
            String expectedCode = attempt < 5 ? "AUTH_UNAUTHORIZED" : "AUTH_LOGIN_TEMPORARILY_LOCKED";
            expectInvalidCredentials("user@example.com", "WrongPassword12!", expectedCode);
        }

        CsrfSession csrfSession = fetchCsrfSession();

        mockMvc.perform(post("/api/v1/auth/login")
                        .cookie(csrfSession.sessionCookie())
                        .contentType(MediaType.APPLICATION_JSON)
                        .header(csrfSession.headerName(), csrfSession.token())
                        .content(objectMapper.writeValueAsString(Map.of(
                                "email", "user@example.com",
                                "password", "Password12!"
                        ))))
                .andExpect(status().isUnauthorized())
                .andExpect(jsonPath("$.code").value("AUTH_LOGIN_TEMPORARILY_LOCKED"));
    }

    @Test
    @DisplayName("잠금 시간이 지나면 올바른 비밀번호로 다시 로그인할 수 있다")
    void loginSucceedsAfterLockExpires() throws Exception {
        userRepository.saveAndFlush(User.createUser(
                "user@example.com",
                passwordEncoder.encode("Password12!"),
                "fromvillage"
        ));

        for (int attempt = 1; attempt <= 5; attempt++) {
            String expectedCode = attempt < 5 ? "AUTH_UNAUTHORIZED" : "AUTH_LOGIN_TEMPORARILY_LOCKED";
            expectInvalidCredentials("user@example.com", "WrongPassword12!", expectedCode);
        }

        clock.advance(Duration.ofMinutes(10).plusSeconds(1));

        var sessionCookie = login("user@example.com", "Password12!");

        mockMvc.perform(get("/test/security/protected").cookie(sessionCookie))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.data").value("ok"));
    }

    @Test
    @DisplayName("로그인 성공 시 이전 실패 횟수와 잠금 상태를 초기화한다")
    void successfulLoginClearsFailureState() throws Exception {
        userRepository.saveAndFlush(User.createUser(
                "user@example.com",
                passwordEncoder.encode("Password12!"),
                "fromvillage"
        ));

        for (int attempt = 1; attempt <= 4; attempt++) {
            expectInvalidCredentials("user@example.com", "WrongPassword12!", "AUTH_UNAUTHORIZED");
        }

        login("user@example.com", "Password12!");
        expectInvalidCredentials("user@example.com", "WrongPassword12!", "AUTH_UNAUTHORIZED");
    }

    @Test
    @DisplayName("형식이 잘못된 로그인 요청은 실패 횟수에 포함하지 않는다")
    void invalidLoginPayloadDoesNotIncreaseFailureCounter() throws Exception {
        userRepository.saveAndFlush(User.createUser(
                "user@example.com",
                passwordEncoder.encode("Password12!"),
                "fromvillage"
        ));

        for (int attempt = 1; attempt <= 4; attempt++) {
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
                    .andExpect(jsonPath("$.code").value("AUTH_UNAUTHORIZED"));
        }

        expectInvalidCredentials("user@example.com", "WrongPassword12!", "AUTH_UNAUTHORIZED");
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
                .andExpect(jsonPath("$.message").value("로그인 요청이 올바르지 않습니다."));
    }

    @Test
    @DisplayName("로그인 요청 본문이 literal null이면 AUTH_UNAUTHORIZED를 반환한다")
    void loginRejectsLiteralNullBody() throws Exception {
        CsrfSession csrfSession = fetchCsrfSession();

        mockMvc.perform(post("/api/v1/auth/login")
                        .cookie(csrfSession.sessionCookie())
                        .contentType(MediaType.APPLICATION_JSON)
                        .header(csrfSession.headerName(), csrfSession.token())
                        .content("null"))
                .andExpect(status().isUnauthorized())
                .andExpect(jsonPath("$.code").value("AUTH_UNAUTHORIZED"))
                .andExpect(jsonPath("$.message").value("로그인 요청 본문이 비어있습니다."));
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
    @DisplayName("ADMIN은 ADMIN 전용 메서드에 접근할 수 있다")
    void adminMethodWithAdminSessionReturnsOk() throws Exception {
        userRepository.saveAndFlush(User.createAdmin(
                "admin@example.com",
                passwordEncoder.encode("Password12!"),
                "admin"
        ));

        var loginSession = login("admin@example.com", "Password12!");

        mockMvc.perform(get("/test/security/admin")
                        .cookie(loginSession))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.data").value("admin"));
    }

    @Test
    @DisplayName("SELLER는 SELLER 전용 메서드에 접근할 수 있다")
    void sellerMethodWithSellerSessionReturnsOk() throws Exception {
        userRepository.saveAndFlush(createSeller(
                "seller@example.com",
                passwordEncoder.encode("Password12!"),
                "seller"
        ));

        var loginSession = login("seller@example.com", "Password12!");

        mockMvc.perform(get("/test/security/seller")
                        .cookie(loginSession))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.data").value("seller"));
    }

    @Test
    @DisplayName("ADMIN은 SELLER 전용 메서드에 접근할 수 없다")
    void sellerMethodWithAdminSessionReturnsForbidden() throws Exception {
        userRepository.saveAndFlush(User.createAdmin(
                "admin@example.com",
                passwordEncoder.encode("Password12!"),
                "admin"
        ));

        var loginSession = login("admin@example.com", "Password12!");

        mockMvc.perform(get("/test/security/seller")
                        .cookie(loginSession))
                .andExpect(status().isForbidden())
                .andExpect(jsonPath("$.code").value("AUTH_FORBIDDEN"));
    }

    @Test
    @DisplayName("로그인으로 세션이 바뀌면 이전 CSRF 토큰은 더 이상 사용할 수 없다")
    void loginRequiresFreshCsrfTokenForSubsequentWrite() throws Exception {
        userRepository.saveAndFlush(User.createUser(
                "user@example.com",
                passwordEncoder.encode("Password12!"),
                "fromvillage"
        ));

        CsrfSession anonymousCsrfSession = fetchCsrfSession();
        var loginSession = login("user@example.com", "Password12!");

        mockMvc.perform(post("/test/security/protected")
                        .cookie(loginSession)
                        .contentType(MediaType.APPLICATION_JSON)
                        .header(anonymousCsrfSession.headerName(), anonymousCsrfSession.token())
                        .content(objectMapper.writeValueAsString(Map.of("value", "stale-token"))))
                .andExpect(status().isForbidden())
                .andExpect(jsonPath("$.code").value("AUTH_CSRF_INVALID"));

        CsrfSession authenticatedCsrfSession = fetchCsrfSession(loginSession);

        mockMvc.perform(post("/test/security/protected")
                        .cookie(authenticatedCsrfSession.sessionCookie())
                        .contentType(MediaType.APPLICATION_JSON)
                        .header(authenticatedCsrfSession.headerName(), authenticatedCsrfSession.token())
                        .content(objectMapper.writeValueAsString(Map.of("value", "fresh-token"))))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.data").value("ok"));
    }

    @Test
    @DisplayName("로그아웃 후 CSRF를 다시 조회하면 새로운 세션과 토큰을 받는다")
    void logoutRequiresFreshCsrfFetchForNextSession() throws Exception {
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

        CsrfSession refreshedCsrf = fetchCsrfSession();

        assertThat(refreshedCsrf.sessionCookie().getValue()).isNotEqualTo(loginSession.getValue());
        assertThat(refreshedCsrf.token()).isNotEqualTo(logoutCsrf.token());

        mockMvc.perform(post("/api/v1/auth/login")
                        .cookie(refreshedCsrf.sessionCookie())
                        .contentType(MediaType.APPLICATION_JSON)
                        .header(refreshedCsrf.headerName(), refreshedCsrf.token())
                        .content(objectMapper.writeValueAsString(Map.of(
                                "email", "user@example.com",
                                "password", "Password12!"
                        ))))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.data.email").value("user@example.com"));
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

    private User createSeller(String email, String password, String nickname) {
        User seller = User.createUser(email, password, nickname);
        seller.approveSeller(LocalDateTime.now(clock));
        return seller;
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

    private void expectInvalidCredentials(String email, String password, String expectedCode) throws Exception {
        CsrfSession csrfSession = fetchCsrfSession();

        mockMvc.perform(post("/api/v1/auth/login")
                        .cookie(csrfSession.sessionCookie())
                        .contentType(MediaType.APPLICATION_JSON)
                        .header(csrfSession.headerName(), csrfSession.token())
                        .content(objectMapper.writeValueAsString(Map.of(
                                "email", email,
                                "password", password
                        ))))
                .andExpect(status().isUnauthorized())
                .andExpect(jsonPath("$.code").value(expectedCode));
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

        @Bean
        @Primary
        MutableClock mutableClock() {
            return new MutableClock(Instant.parse("2026-03-09T00:00:00Z"), ZoneId.of("UTC"));
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

        @GetMapping("/seller")
        ApiResponse<String> sellerOnly() {
            return ApiResponse.success(securityProbeService.sellerOnly());
        }
    }

    static class SecurityProbeService {

        @PreAuthorize("hasRole('ADMIN')")
        String adminOnly() {
            return "admin";
        }

        @PreAuthorize("hasRole('SELLER')")
        String sellerOnly() {
            return "seller";
        }
    }

    static final class MutableClock extends Clock {

        private Instant currentInstant;
        private final ZoneId zoneId;

        MutableClock(Instant currentInstant, ZoneId zoneId) {
            this.currentInstant = currentInstant;
            this.zoneId = zoneId;
        }

        @Override
        public ZoneId getZone() {
            return zoneId;
        }

        @Override
        public Clock withZone(ZoneId zone) {
            return new MutableClock(currentInstant, zone);
        }

        @Override
        public Instant instant() {
            return currentInstant;
        }

        void advance(Duration duration) {
            currentInstant = currentInstant.plus(duration);
        }

        void reset(Instant instant) {
            currentInstant = instant;
        }
    }
}
