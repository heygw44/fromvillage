package com.fromvillage.coupon.presentation;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.fromvillage.coupon.domain.CouponPolicy;
import com.fromvillage.coupon.infrastructure.CouponPolicyJpaRepository;
import com.fromvillage.cart.infrastructure.CartJpaRepository;
import com.fromvillage.order.infrastructure.CheckoutOrderJpaRepository;
import com.fromvillage.order.infrastructure.SellerOrderJpaRepository;
import com.fromvillage.product.infrastructure.ProductJpaRepository;
import com.fromvillage.support.TestContainersConfig;
import com.fromvillage.user.domain.User;
import com.fromvillage.user.infrastructure.UserJpaRepository;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.context.annotation.Import;
import org.springframework.data.redis.core.StringRedisTemplate;
import org.springframework.http.MediaType;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.MvcResult;

import java.time.LocalDateTime;
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
@Import(TestContainersConfig.class)
class AdminCouponPolicyIntegrationTest {

    private static final String SESSION_NAMESPACE = "fromvillage:session:*";
    private static final String LOGIN_FAILURE_NAMESPACE = "fromvillage:auth:login-failure:*";

    @Autowired
    private MockMvc mockMvc;

    @Autowired
    private ObjectMapper objectMapper;

    @Autowired
    private UserJpaRepository userRepository;

    @Autowired
    private CouponPolicyJpaRepository couponPolicyRepository;

    @Autowired
    private ProductJpaRepository productRepository;

    @Autowired
    private CartJpaRepository cartRepository;

    @Autowired
    private SellerOrderJpaRepository sellerOrderRepository;

    @Autowired
    private CheckoutOrderJpaRepository checkoutOrderRepository;

    @Autowired
    private PasswordEncoder passwordEncoder;

    @Autowired
    private StringRedisTemplate stringRedisTemplate;

    @BeforeEach
    void setUp() {
        cartRepository.deleteAll();
        sellerOrderRepository.deleteAll();
        checkoutOrderRepository.deleteAll();
        couponPolicyRepository.deleteAll();
        productRepository.deleteAll();
        userRepository.deleteAll();

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
    @DisplayName("ADMIN은 쿠폰 정책을 생성할 수 있다")
    void adminCanCreateCouponPolicy() throws Exception {
        userRepository.saveAndFlush(createAdmin("admin@example.com", "운영자"));

        var adminSession = login("admin@example.com", "Password12!");
        var csrfSession = fetchCsrfSession(adminSession);

        mockMvc.perform(post("/api/v1/admin/coupon-policies")
                        .cookie(adminSession)
                        .contentType(MediaType.APPLICATION_JSON)
                        .header(csrfSession.headerName(), csrfSession.token())
                        .content(objectMapper.writeValueAsString(validCreateRequest())))
                .andExpect(status().isCreated())
                .andExpect(jsonPath("$.success").value(true))
                .andExpect(jsonPath("$.data.couponPolicyId").isNumber())
                .andExpect(jsonPath("$.data.name").value("봄맞이 할인"))
                .andExpect(jsonPath("$.data.discountAmount").value(3000))
                .andExpect(jsonPath("$.data.minimumOrderAmount").value(20000))
                .andExpect(jsonPath("$.data.totalQuantity").value(100))
                .andExpect(jsonPath("$.data.issuedQuantity").value(0))
                .andExpect(jsonPath("$.data.status").value("READY"))
                .andExpect(jsonPath("$.data.startedAt").value("2026-03-20T00:00:00"))
                .andExpect(jsonPath("$.data.endedAt").value("2026-03-31T23:59:00"));

        assertThat(couponPolicyRepository.findAll()).hasSize(1);
        assertThat(couponPolicyRepository.findAll().getFirst().getStatus().name()).isEqualTo("READY");
    }

    @Test
    @DisplayName("ADMIN은 READY 상태의 쿠폰 정책을 OPEN으로 전이할 수 있다")
    void adminCanOpenCouponPolicy() throws Exception {
        User admin = userRepository.saveAndFlush(createAdmin("admin@example.com", "운영자"));
        CouponPolicy couponPolicy = couponPolicyRepository.saveAndFlush(createPolicy(admin));

        var adminSession = login("admin@example.com", "Password12!");
        var csrfSession = fetchCsrfSession(adminSession);

        mockMvc.perform(post("/api/v1/admin/coupon-policies/{couponPolicyId}/open", couponPolicy.getId())
                        .cookie(adminSession)
                        .contentType(MediaType.APPLICATION_JSON)
                        .header(csrfSession.headerName(), csrfSession.token()))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.success").value(true))
                .andExpect(jsonPath("$.data.couponPolicyId").value(couponPolicy.getId()))
                .andExpect(jsonPath("$.data.status").value("OPEN"));

        CouponPolicy openedPolicy = couponPolicyRepository.findById(couponPolicy.getId()).orElseThrow();
        assertThat(openedPolicy.getStatus().name()).isEqualTo("OPEN");
    }

    @Test
    @DisplayName("ADMIN은 READY 상태의 쿠폰 정책을 CLOSED로 전이할 수 있다")
    void adminCanCloseReadyCouponPolicy() throws Exception {
        User admin = userRepository.saveAndFlush(createAdmin("admin@example.com", "운영자"));
        CouponPolicy couponPolicy = couponPolicyRepository.saveAndFlush(createPolicy(admin));

        var adminSession = login("admin@example.com", "Password12!");
        var csrfSession = fetchCsrfSession(adminSession);

        mockMvc.perform(post("/api/v1/admin/coupon-policies/{couponPolicyId}/close", couponPolicy.getId())
                        .cookie(adminSession)
                        .contentType(MediaType.APPLICATION_JSON)
                        .header(csrfSession.headerName(), csrfSession.token()))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.success").value(true))
                .andExpect(jsonPath("$.data.couponPolicyId").value(couponPolicy.getId()))
                .andExpect(jsonPath("$.data.status").value("CLOSED"));

        CouponPolicy closedPolicy = couponPolicyRepository.findById(couponPolicy.getId()).orElseThrow();
        assertThat(closedPolicy.getStatus().name()).isEqualTo("CLOSED");
    }

    @Test
    @DisplayName("ADMIN은 OPEN 상태의 쿠폰 정책을 CLOSED로 전이할 수 있다")
    void adminCanCloseOpenCouponPolicy() throws Exception {
        User admin = userRepository.saveAndFlush(createAdmin("admin@example.com", "운영자"));
        CouponPolicy couponPolicy = createPolicy(admin);
        couponPolicy.open();
        couponPolicy = couponPolicyRepository.saveAndFlush(couponPolicy);

        var adminSession = login("admin@example.com", "Password12!");
        var csrfSession = fetchCsrfSession(adminSession);

        mockMvc.perform(post("/api/v1/admin/coupon-policies/{couponPolicyId}/close", couponPolicy.getId())
                        .cookie(adminSession)
                        .contentType(MediaType.APPLICATION_JSON)
                        .header(csrfSession.headerName(), csrfSession.token()))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.success").value(true))
                .andExpect(jsonPath("$.data.couponPolicyId").value(couponPolicy.getId()))
                .andExpect(jsonPath("$.data.status").value("CLOSED"));

        CouponPolicy closedPolicy = couponPolicyRepository.findById(couponPolicy.getId()).orElseThrow();
        assertThat(closedPolicy.getStatus().name()).isEqualTo("CLOSED");
    }

    @Test
    @DisplayName("미인증 요청은 쿠폰 정책 생성에서 AUTH_UNAUTHORIZED를 반환한다")
    void unauthenticatedCreateRequestIsRejected() throws Exception {
        var csrfSession = fetchCsrfSession();

        mockMvc.perform(post("/api/v1/admin/coupon-policies")
                        .cookie(csrfSession.sessionCookie())
                        .contentType(MediaType.APPLICATION_JSON)
                        .header(csrfSession.headerName(), csrfSession.token())
                        .content(objectMapper.writeValueAsString(validCreateRequest())))
                .andExpect(status().isUnauthorized())
                .andExpect(jsonPath("$.code").value("AUTH_UNAUTHORIZED"));
    }

    @Test
    @DisplayName("USER는 쿠폰 정책 생성 API에 접근할 수 없다")
    void userCannotCreateCouponPolicy() throws Exception {
        userRepository.saveAndFlush(createUser("user@example.com", "일반회원"));

        var userSession = login("user@example.com", "Password12!");
        var csrfSession = fetchCsrfSession(userSession);

        mockMvc.perform(post("/api/v1/admin/coupon-policies")
                        .cookie(userSession)
                        .contentType(MediaType.APPLICATION_JSON)
                        .header(csrfSession.headerName(), csrfSession.token())
                        .content(objectMapper.writeValueAsString(validCreateRequest())))
                .andExpect(status().isForbidden())
                .andExpect(jsonPath("$.code").value("AUTH_FORBIDDEN"));
    }

    @Test
    @DisplayName("쿠폰 정책 생성 API는 CSRF 토큰이 없으면 거절된다")
    void createCouponPolicyRequiresCsrf() throws Exception {
        userRepository.saveAndFlush(createAdmin("admin@example.com", "운영자"));

        var adminSession = login("admin@example.com", "Password12!");

        mockMvc.perform(post("/api/v1/admin/coupon-policies")
                        .cookie(adminSession)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(validCreateRequest())))
                .andExpect(status().isForbidden())
                .andExpect(jsonPath("$.code").value("AUTH_CSRF_INVALID"));
    }

    @Test
    @DisplayName("할인 금액이 0 이하이면 요청 단계에서 거절된다")
    void createCouponPolicyRejectsNonPositiveDiscountAmount() throws Exception {
        userRepository.saveAndFlush(createAdmin("admin@example.com", "운영자"));

        var adminSession = login("admin@example.com", "Password12!");
        var csrfSession = fetchCsrfSession(adminSession);

        mockMvc.perform(post("/api/v1/admin/coupon-policies")
                        .cookie(adminSession)
                        .contentType(MediaType.APPLICATION_JSON)
                        .header(csrfSession.headerName(), csrfSession.token())
                        .content(objectMapper.writeValueAsString(Map.of(
                                "name", "봄맞이 할인",
                                "discountAmount", 0,
                                "minimumOrderAmount", 20000,
                                "totalQuantity", 100,
                                "startedAt", "2026-03-20T00:00:00",
                                "endedAt", "2026-03-31T23:59:00"
                        ))))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.code").value("VALIDATION_ERROR"))
                .andExpect(jsonPath("$.errors[0].reason").value("할인 금액은 1원 이상이어야 합니다."));
    }

    @Test
    @DisplayName("최소 주문 금액이 0 미만이면 요청 단계에서 거절된다")
    void createCouponPolicyRejectsNegativeMinimumOrderAmount() throws Exception {
        userRepository.saveAndFlush(createAdmin("admin@example.com", "운영자"));

        var adminSession = login("admin@example.com", "Password12!");
        var csrfSession = fetchCsrfSession(adminSession);

        mockMvc.perform(post("/api/v1/admin/coupon-policies")
                        .cookie(adminSession)
                        .contentType(MediaType.APPLICATION_JSON)
                        .header(csrfSession.headerName(), csrfSession.token())
                        .content(objectMapper.writeValueAsString(Map.of(
                                "name", "봄맞이 할인",
                                "discountAmount", 3000,
                                "minimumOrderAmount", -1,
                                "totalQuantity", 100,
                                "startedAt", "2026-03-20T00:00:00",
                                "endedAt", "2026-03-31T23:59:00"
                        ))))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.code").value("VALIDATION_ERROR"))
                .andExpect(jsonPath("$.errors[0].reason").value("최소 주문 금액은 0원 이상이어야 합니다."));
    }

    @Test
    @DisplayName("총 발급 수량이 0 이하이면 요청 단계에서 거절된다")
    void createCouponPolicyRejectsNonPositiveTotalQuantity() throws Exception {
        userRepository.saveAndFlush(createAdmin("admin@example.com", "운영자"));

        var adminSession = login("admin@example.com", "Password12!");
        var csrfSession = fetchCsrfSession(adminSession);

        mockMvc.perform(post("/api/v1/admin/coupon-policies")
                        .cookie(adminSession)
                        .contentType(MediaType.APPLICATION_JSON)
                        .header(csrfSession.headerName(), csrfSession.token())
                        .content(objectMapper.writeValueAsString(Map.of(
                                "name", "봄맞이 할인",
                                "discountAmount", 3000,
                                "minimumOrderAmount", 20000,
                                "totalQuantity", 0,
                                "startedAt", "2026-03-20T00:00:00",
                                "endedAt", "2026-03-31T23:59:00"
                        ))))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.code").value("VALIDATION_ERROR"))
                .andExpect(jsonPath("$.errors[0].reason").value("총 발급 수량은 1개 이상이어야 합니다."));
    }

    @Test
    @DisplayName("발급 기간이 올바르지 않으면 요청 단계에서 거절된다")
    void createCouponPolicyRejectsInvalidIssuePeriod() throws Exception {
        userRepository.saveAndFlush(createAdmin("admin@example.com", "운영자"));

        var adminSession = login("admin@example.com", "Password12!");
        var csrfSession = fetchCsrfSession(adminSession);

        mockMvc.perform(post("/api/v1/admin/coupon-policies")
                        .cookie(adminSession)
                        .contentType(MediaType.APPLICATION_JSON)
                        .header(csrfSession.headerName(), csrfSession.token())
                        .content(objectMapper.writeValueAsString(Map.of(
                                "name", "봄맞이 할인",
                                "discountAmount", 3000,
                                "minimumOrderAmount", 20000,
                                "totalQuantity", 100,
                                "startedAt", "2026-03-31T23:59:00",
                                "endedAt", "2026-03-31T23:59:00"
                        ))))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.code").value("VALIDATION_ERROR"))
                .andExpect(jsonPath("$.errors[0].reason").value("쿠폰 발급 기간을 다시 확인해 주세요."));
    }

    @Test
    @DisplayName("필수값이 없으면 요청 단계에서 거절된다")
    void createCouponPolicyRejectsMissingRequiredField() throws Exception {
        userRepository.saveAndFlush(createAdmin("admin@example.com", "운영자"));

        var adminSession = login("admin@example.com", "Password12!");
        var csrfSession = fetchCsrfSession(adminSession);

        mockMvc.perform(post("/api/v1/admin/coupon-policies")
                        .cookie(adminSession)
                        .contentType(MediaType.APPLICATION_JSON)
                        .header(csrfSession.headerName(), csrfSession.token())
                        .content(objectMapper.writeValueAsString(Map.of(
                                "discountAmount", 3000,
                                "minimumOrderAmount", 20000,
                                "totalQuantity", 100,
                                "startedAt", "2026-03-20T00:00:00",
                                "endedAt", "2026-03-31T23:59:00"
                        ))))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.code").value("VALIDATION_ERROR"))
                .andExpect(jsonPath("$.errors[0].reason").value("쿠폰명이 입력되지 않았습니다."));
    }

    @Test
    @DisplayName("존재하지 않는 쿠폰 정책을 오픈하면 COUPON_POLICY_NOT_FOUND를 반환한다")
    void openCouponPolicyRejectsMissingPolicy() throws Exception {
        userRepository.saveAndFlush(createAdmin("admin@example.com", "운영자"));

        var adminSession = login("admin@example.com", "Password12!");
        var csrfSession = fetchCsrfSession(adminSession);

        mockMvc.perform(post("/api/v1/admin/coupon-policies/{couponPolicyId}/open", 9999L)
                        .cookie(adminSession)
                        .contentType(MediaType.APPLICATION_JSON)
                        .header(csrfSession.headerName(), csrfSession.token()))
                .andExpect(status().isNotFound())
                .andExpect(jsonPath("$.code").value("COUPON_POLICY_NOT_FOUND"));
    }

    @Test
    @DisplayName("존재하지 않는 쿠폰 정책을 종료하면 COUPON_POLICY_NOT_FOUND를 반환한다")
    void closeCouponPolicyRejectsMissingPolicy() throws Exception {
        userRepository.saveAndFlush(createAdmin("admin@example.com", "운영자"));

        var adminSession = login("admin@example.com", "Password12!");
        var csrfSession = fetchCsrfSession(adminSession);

        mockMvc.perform(post("/api/v1/admin/coupon-policies/{couponPolicyId}/close", 9999L)
                        .cookie(adminSession)
                        .contentType(MediaType.APPLICATION_JSON)
                        .header(csrfSession.headerName(), csrfSession.token()))
                .andExpect(status().isNotFound())
                .andExpect(jsonPath("$.code").value("COUPON_POLICY_NOT_FOUND"));
    }

    @Test
    @DisplayName("이미 OPEN인 쿠폰 정책은 다시 오픈할 수 없다")
    void openCouponPolicyRejectsAlreadyOpen() throws Exception {
        User admin = userRepository.saveAndFlush(createAdmin("admin@example.com", "운영자"));
        CouponPolicy couponPolicy = createPolicy(admin);
        couponPolicy.open();
        couponPolicy = couponPolicyRepository.saveAndFlush(couponPolicy);

        var adminSession = login("admin@example.com", "Password12!");
        var csrfSession = fetchCsrfSession(adminSession);

        mockMvc.perform(post("/api/v1/admin/coupon-policies/{couponPolicyId}/open", couponPolicy.getId())
                        .cookie(adminSession)
                        .contentType(MediaType.APPLICATION_JSON)
                        .header(csrfSession.headerName(), csrfSession.token()))
                .andExpect(status().isConflict())
                .andExpect(jsonPath("$.code").value("COUPON_POLICY_STATUS_TRANSITION_INVALID"));
    }

    @Test
    @DisplayName("이미 CLOSED인 쿠폰 정책은 다시 종료할 수 없다")
    void closeCouponPolicyRejectsAlreadyClosed() throws Exception {
        User admin = userRepository.saveAndFlush(createAdmin("admin@example.com", "운영자"));
        CouponPolicy couponPolicy = createPolicy(admin);
        couponPolicy.close();
        couponPolicy = couponPolicyRepository.saveAndFlush(couponPolicy);

        var adminSession = login("admin@example.com", "Password12!");
        var csrfSession = fetchCsrfSession(adminSession);

        mockMvc.perform(post("/api/v1/admin/coupon-policies/{couponPolicyId}/close", couponPolicy.getId())
                        .cookie(adminSession)
                        .contentType(MediaType.APPLICATION_JSON)
                        .header(csrfSession.headerName(), csrfSession.token()))
                .andExpect(status().isConflict())
                .andExpect(jsonPath("$.code").value("COUPON_POLICY_STATUS_TRANSITION_INVALID"));
    }

    @Test
    @DisplayName("CLOSED 상태의 쿠폰 정책은 다시 오픈할 수 없다")
    void openCouponPolicyRejectsClosedPolicy() throws Exception {
        User admin = userRepository.saveAndFlush(createAdmin("admin@example.com", "운영자"));
        CouponPolicy couponPolicy = createPolicy(admin);
        couponPolicy.close();
        couponPolicy = couponPolicyRepository.saveAndFlush(couponPolicy);

        var adminSession = login("admin@example.com", "Password12!");
        var csrfSession = fetchCsrfSession(adminSession);

        mockMvc.perform(post("/api/v1/admin/coupon-policies/{couponPolicyId}/open", couponPolicy.getId())
                        .cookie(adminSession)
                        .contentType(MediaType.APPLICATION_JSON)
                        .header(csrfSession.headerName(), csrfSession.token()))
                .andExpect(status().isConflict())
                .andExpect(jsonPath("$.code").value("COUPON_POLICY_STATUS_TRANSITION_INVALID"));
    }

    private User createAdmin(String email, String nickname) {
        return User.createAdmin(email, passwordEncoder.encode("Password12!"), nickname);
    }

    private User createUser(String email, String nickname) {
        return User.createUser(email, passwordEncoder.encode("Password12!"), nickname);
    }

    private CouponPolicy createPolicy(User admin) {
        return CouponPolicy.create(
                "봄맞이 할인",
                3000L,
                20000L,
                100,
                LocalDateTime.of(2026, 3, 20, 0, 0),
                LocalDateTime.of(2026, 3, 31, 23, 59),
                admin
        );
    }

    private Map<String, Object> validCreateRequest() {
        return Map.of(
                "name", "봄맞이 할인",
                "discountAmount", 3000,
                "minimumOrderAmount", 20000,
                "totalQuantity", 100,
                "startedAt", "2026-03-20T00:00:00",
                "endedAt", "2026-03-31T23:59:00"
        );
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
}
