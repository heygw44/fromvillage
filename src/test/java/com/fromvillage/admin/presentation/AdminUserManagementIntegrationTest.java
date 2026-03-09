package com.fromvillage.admin.presentation;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
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
import org.springframework.context.annotation.Primary;
import org.springframework.data.redis.core.StringRedisTemplate;
import org.springframework.http.MediaType;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.MvcResult;

import java.time.Clock;
import java.time.Instant;
import java.time.LocalDateTime;
import java.time.ZoneId;
import java.util.Map;
import java.util.Set;

import static org.assertj.core.api.Assertions.assertThat;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

@SpringBootTest
@AutoConfigureMockMvc
@ActiveProfiles("test")
@Import({
        TestContainersConfig.class,
        AdminUserManagementIntegrationTest.TestClockConfig.class
})
class AdminUserManagementIntegrationTest {

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
    @DisplayName("ADMIN은 전체 회원 기본 목록을 페이지 형태로 조회할 수 있다")
    void adminCanListUsers() throws Exception {
        User admin = userRepository.saveAndFlush(User.createAdmin(
                "admin@example.com",
                passwordEncoder.encode("Password12!"),
                "운영자"
        ));
        User user = userRepository.saveAndFlush(User.createUser(
                "user@example.com",
                passwordEncoder.encode("Password12!"),
                "일반회원"
        ));
        User seller = User.createUser(
                "seller@example.com",
                passwordEncoder.encode("Password12!"),
                "판매자"
        );
        seller.approveSeller(LocalDateTime.now(clock));
        seller = userRepository.saveAndFlush(seller);

        var adminSession = login("admin@example.com", "Password12!");

        mockMvc.perform(get("/api/v1/admin/users")
                        .cookie(adminSession)
                        .param("page", "0")
                        .param("size", "10")
                        .param("sort", "createdAt,asc"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.success").value(true))
                .andExpect(jsonPath("$.data.content.length()").value(3))
                .andExpect(jsonPath("$.data.content[0].userId").value(admin.getId()))
                .andExpect(jsonPath("$.data.content[0].email").value("admin@example.com"))
                .andExpect(jsonPath("$.data.content[0].nickname").value("운영자"))
                .andExpect(jsonPath("$.data.content[0].role").value("ADMIN"))
                .andExpect(jsonPath("$.data.content[0].password").doesNotExist())
                .andExpect(jsonPath("$.data.content[1].userId").value(user.getId()))
                .andExpect(jsonPath("$.data.content[1].role").value("USER"))
                .andExpect(jsonPath("$.data.content[2].userId").value(seller.getId()))
                .andExpect(jsonPath("$.data.content[2].role").value("SELLER"))
                .andExpect(jsonPath("$.data.content[2].sellerApprovedAt").value("2026-03-09T00:00:00"))
                .andExpect(jsonPath("$.data.page").value(0))
                .andExpect(jsonPath("$.data.size").value(10))
                .andExpect(jsonPath("$.data.totalElements").value(3))
                .andExpect(jsonPath("$.data.totalPages").value(1))
                .andExpect(jsonPath("$.data.hasNext").value(false));
    }

    @Test
    @DisplayName("미인증 요청은 관리자 회원 목록 조회에서 AUTH_UNAUTHORIZED를 반환한다")
    void unauthenticatedListRequestIsRejected() throws Exception {
        mockMvc.perform(get("/api/v1/admin/users"))
                .andExpect(status().isUnauthorized())
                .andExpect(jsonPath("$.code").value("AUTH_UNAUTHORIZED"));
    }

    @Test
    @DisplayName("USER는 관리자 회원 목록 조회에 접근할 수 없다")
    void userCannotListUsers() throws Exception {
        userRepository.saveAndFlush(User.createUser(
                "user@example.com",
                passwordEncoder.encode("Password12!"),
                "일반회원"
        ));

        var userSession = login("user@example.com", "Password12!");

        mockMvc.perform(get("/api/v1/admin/users")
                        .cookie(userSession))
                .andExpect(status().isForbidden())
                .andExpect(jsonPath("$.code").value("AUTH_FORBIDDEN"));
    }

    @Test
    @DisplayName("ADMIN은 USER를 SELLER로 전환하고 승인 시각을 응답한다")
    void adminCanApproveSellerRole() throws Exception {
        userRepository.saveAndFlush(User.createAdmin(
                "admin@example.com",
                passwordEncoder.encode("Password12!"),
                "운영자"
        ));
        User user = userRepository.saveAndFlush(User.createUser(
                "user@example.com",
                passwordEncoder.encode("Password12!"),
                "일반회원"
        ));

        var adminSession = login("admin@example.com", "Password12!");
        var csrfSession = fetchCsrfSession(adminSession);

        mockMvc.perform(post("/api/v1/admin/users/{userId}/seller-role", user.getId())
                        .cookie(adminSession)
                        .contentType(MediaType.APPLICATION_JSON)
                        .header(csrfSession.headerName(), csrfSession.token()))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.success").value(true))
                .andExpect(jsonPath("$.data.userId").value(user.getId()))
                .andExpect(jsonPath("$.data.role").value("SELLER"))
                .andExpect(jsonPath("$.data.sellerApprovedAt").value("2026-03-09T00:00:00"));

        User approvedUser = userRepository.findById(user.getId()).orElseThrow();
        assertThat(approvedUser.getRole().name()).isEqualTo("SELLER");
        assertThat(approvedUser.getSellerApprovedAt()).isEqualTo(LocalDateTime.of(2026, 3, 9, 0, 0));
    }

    @Test
    @DisplayName("USER는 판매자 권한 부여 API에 접근할 수 없다")
    void userCannotApproveSellerRole() throws Exception {
        User target = userRepository.saveAndFlush(User.createUser(
                "target@example.com",
                passwordEncoder.encode("Password12!"),
                "대상"
        ));
        userRepository.saveAndFlush(User.createUser(
                "user@example.com",
                passwordEncoder.encode("Password12!"),
                "일반회원"
        ));

        var userSession = login("user@example.com", "Password12!");
        var csrfSession = fetchCsrfSession(userSession);

        mockMvc.perform(post("/api/v1/admin/users/{userId}/seller-role", target.getId())
                        .cookie(userSession)
                        .contentType(MediaType.APPLICATION_JSON)
                        .header(csrfSession.headerName(), csrfSession.token()))
                .andExpect(status().isForbidden())
                .andExpect(jsonPath("$.code").value("AUTH_FORBIDDEN"));
    }

    @Test
    @DisplayName("판매자 권한 부여 API는 CSRF 토큰이 없으면 거절된다")
    void approveSellerRoleRequiresCsrf() throws Exception {
        userRepository.saveAndFlush(User.createAdmin(
                "admin@example.com",
                passwordEncoder.encode("Password12!"),
                "운영자"
        ));
        User target = userRepository.saveAndFlush(User.createUser(
                "user@example.com",
                passwordEncoder.encode("Password12!"),
                "일반회원"
        ));

        var adminSession = login("admin@example.com", "Password12!");

        mockMvc.perform(post("/api/v1/admin/users/{userId}/seller-role", target.getId())
                        .cookie(adminSession)
                        .contentType(MediaType.APPLICATION_JSON))
                .andExpect(status().isForbidden())
                .andExpect(jsonPath("$.code").value("AUTH_CSRF_INVALID"));
    }

    @Test
    @DisplayName("이미 SELLER인 계정은 다시 승인할 수 없다")
    void approveSellerRoleRejectsAlreadySeller() throws Exception {
        userRepository.saveAndFlush(User.createAdmin(
                "admin@example.com",
                passwordEncoder.encode("Password12!"),
                "운영자"
        ));
        User seller = User.createUser(
                "seller@example.com",
                passwordEncoder.encode("Password12!"),
                "판매자"
        );
        seller.approveSeller(LocalDateTime.now(clock));
        seller = userRepository.saveAndFlush(seller);

        var adminSession = login("admin@example.com", "Password12!");
        var csrfSession = fetchCsrfSession(adminSession);

        mockMvc.perform(post("/api/v1/admin/users/{userId}/seller-role", seller.getId())
                        .cookie(adminSession)
                        .contentType(MediaType.APPLICATION_JSON)
                        .header(csrfSession.headerName(), csrfSession.token()))
                .andExpect(status().isConflict())
                .andExpect(jsonPath("$.code").value("USER_ALREADY_SELLER"));
    }

    @Test
    @DisplayName("ADMIN 계정은 SELLER로 전환할 수 없다")
    void approveSellerRoleRejectsAdminTarget() throws Exception {
        userRepository.saveAndFlush(User.createAdmin(
                "admin@example.com",
                passwordEncoder.encode("Password12!"),
                "운영자"
        ));
        User targetAdmin = userRepository.saveAndFlush(User.createAdmin(
                "other-admin@example.com",
                passwordEncoder.encode("Password12!"),
                "다른운영자"
        ));

        var adminSession = login("admin@example.com", "Password12!");
        var csrfSession = fetchCsrfSession(adminSession);

        mockMvc.perform(post("/api/v1/admin/users/{userId}/seller-role", targetAdmin.getId())
                        .cookie(adminSession)
                        .contentType(MediaType.APPLICATION_JSON)
                        .header(csrfSession.headerName(), csrfSession.token()))
                .andExpect(status().isConflict())
                .andExpect(jsonPath("$.code").value("SELLER_APPROVAL_NOT_ALLOWED"));
    }

    @Test
    @DisplayName("존재하지 않는 사용자에게 판매자 권한을 부여하면 USER_NOT_FOUND를 반환한다")
    void approveSellerRoleRejectsMissingUser() throws Exception {
        userRepository.saveAndFlush(User.createAdmin(
                "admin@example.com",
                passwordEncoder.encode("Password12!"),
                "운영자"
        ));

        var adminSession = login("admin@example.com", "Password12!");
        var csrfSession = fetchCsrfSession(adminSession);

        mockMvc.perform(post("/api/v1/admin/users/{userId}/seller-role", 9999L)
                        .cookie(adminSession)
                        .contentType(MediaType.APPLICATION_JSON)
                        .header(csrfSession.headerName(), csrfSession.token()))
                .andExpect(status().isNotFound())
                .andExpect(jsonPath("$.code").value("USER_NOT_FOUND"));
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

        return toCsrfSession(result, null);
    }

    private CsrfSession fetchCsrfSession(jakarta.servlet.http.Cookie sessionCookie) throws Exception {
        MvcResult result = mockMvc.perform(get("/api/v1/csrf").cookie(sessionCookie))
                .andExpect(status().isOk())
                .andReturn();

        return toCsrfSession(result, sessionCookie);
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
    static class TestClockConfig {

        @Bean
        @Primary
        MutableClock mutableClock() {
            return new MutableClock(Instant.parse("2026-03-09T00:00:00Z"), ZoneId.of("UTC"));
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

        void reset(Instant instant) {
            currentInstant = instant;
        }
    }
}
