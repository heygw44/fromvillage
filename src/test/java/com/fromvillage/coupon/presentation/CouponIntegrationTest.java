package com.fromvillage.coupon.presentation;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.fromvillage.cart.infrastructure.CartJpaRepository;
import com.fromvillage.coupon.domain.CouponPolicy;
import com.fromvillage.coupon.domain.IssuedCoupon;
import com.fromvillage.coupon.infrastructure.CouponPolicyJpaRepository;
import com.fromvillage.coupon.infrastructure.IssuedCouponJpaRepository;
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
        CouponIntegrationTest.TestClockConfig.class
})
class CouponIntegrationTest {

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
    private IssuedCouponJpaRepository issuedCouponRepository;

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

    @Autowired
    private MutableClock clock;

    @BeforeEach
    void setUp() {
        cartRepository.deleteAll();
        sellerOrderRepository.deleteAll();
        checkoutOrderRepository.deleteAll();
        issuedCouponRepository.deleteAll();
        couponPolicyRepository.deleteAll();
        productRepository.deleteAll();
        userRepository.deleteAll();
        clock.reset(Instant.parse("2026-03-15T10:00:00Z"));

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
    @DisplayName("USER는 OPEN 상태의 쿠폰을 발급받을 수 있다")
    void userCanIssueCoupon() throws Exception {
        User user = userRepository.saveAndFlush(createUser("user@example.com", "일반회원"));
        User admin = userRepository.saveAndFlush(createAdmin("admin@example.com", "운영자"));
        CouponPolicy couponPolicy = couponPolicyRepository.saveAndFlush(createOpenPolicy(admin));

        var userSession = login("user@example.com", "Password12!");
        var csrfSession = fetchCsrfSession(userSession);

        mockMvc.perform(post("/api/v1/coupons/{couponPolicyId}/issue", couponPolicy.getId())
                        .cookie(userSession)
                        .contentType(MediaType.APPLICATION_JSON)
                        .header(csrfSession.headerName(), csrfSession.token()))
                .andExpect(status().isCreated())
                .andExpect(jsonPath("$.success").value(true))
                .andExpect(jsonPath("$.data.issuedCouponId").isNumber())
                .andExpect(jsonPath("$.data.couponPolicyId").value(couponPolicy.getId()))
                .andExpect(jsonPath("$.data.couponName").value("봄맞이 할인"))
                .andExpect(jsonPath("$.data.discountAmount").value(3000))
                .andExpect(jsonPath("$.data.minimumOrderAmount").value(20000))
                .andExpect(jsonPath("$.data.status").value("ISSUED"))
                .andExpect(jsonPath("$.data.issuedAt").value("2026-03-15T10:00:00"))
                .andExpect(jsonPath("$.data.startedAt").value("2026-03-10T00:00:00"))
                .andExpect(jsonPath("$.data.endedAt").value("2026-03-31T23:59:00"));

        assertThat(issuedCouponRepository.findAll()).hasSize(1);
        IssuedCoupon issuedCoupon = issuedCouponRepository.findAll().getFirst();
        assertThat(issuedCoupon.getCouponPolicy().getId()).isEqualTo(couponPolicy.getId());
        assertThat(issuedCoupon.getUser().getId()).isEqualTo(user.getId());

        CouponPolicy updatedPolicy = couponPolicyRepository.findById(couponPolicy.getId()).orElseThrow();
        assertThat(updatedPolicy.getIssuedQuantity()).isEqualTo(1);
    }

    @Test
    @DisplayName("USER는 자신의 ISSUED 쿠폰만 보유 목록으로 조회할 수 있다")
    void userCanGetMyCoupons() throws Exception {
        User user = userRepository.saveAndFlush(createUser("user@example.com", "일반회원"));
        User otherUser = userRepository.saveAndFlush(createUser("other@example.com", "다른회원"));
        User admin = userRepository.saveAndFlush(createAdmin("admin@example.com", "운영자"));

        CouponPolicy firstPolicy = couponPolicyRepository.saveAndFlush(createOpenPolicy(
                admin,
                "봄맞이 할인",
                3000L,
                20000L,
                100
        ));
        CouponPolicy secondPolicy = couponPolicyRepository.saveAndFlush(createOpenPolicy(
                admin,
                "주말 할인",
                5000L,
                30000L,
                50
        ));
        CouponPolicy thirdPolicy = couponPolicyRepository.saveAndFlush(createOpenPolicy(
                admin,
                "평일 할인",
                2000L,
                15000L,
                30
        ));

        issuedCouponRepository.saveAndFlush(IssuedCoupon.issue(
                firstPolicy,
                user,
                LocalDateTime.of(2026, 3, 15, 9, 0)
        ));

        issuedCouponRepository.saveAndFlush(IssuedCoupon.issue(
                secondPolicy,
                user,
                LocalDateTime.of(2026, 3, 15, 10, 0)
        ));

        IssuedCoupon usedCoupon = issuedCouponRepository.saveAndFlush(IssuedCoupon.issue(
                thirdPolicy,
                user,
                LocalDateTime.of(2026, 3, 15, 11, 0)
        ));
        usedCoupon.use(LocalDateTime.of(2026, 3, 16, 12, 0));
        issuedCouponRepository.saveAndFlush(usedCoupon);

        issuedCouponRepository.saveAndFlush(IssuedCoupon.issue(
                firstPolicy,
                otherUser,
                LocalDateTime.of(2026, 3, 15, 12, 0)
        ));

        var userSession = login("user@example.com", "Password12!");

        mockMvc.perform(get("/api/v1/coupons/me")
                        .cookie(userSession))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.success").value(true))
                .andExpect(jsonPath("$.data.coupons.length()").value(2))
                .andExpect(jsonPath("$.data.coupons[0].couponName").value("주말 할인"))
                .andExpect(jsonPath("$.data.coupons[0].status").value("ISSUED"))
                .andExpect(jsonPath("$.data.coupons[1].couponName").value("봄맞이 할인"))
                .andExpect(jsonPath("$.data.coupons[1].status").value("ISSUED"));
    }

    @Test
    @DisplayName("같은 USER는 같은 쿠폰 정책을 중복 발급받을 수 없다")
    void userCannotIssueSameCouponTwice() throws Exception {
        User user = userRepository.saveAndFlush(createUser("user@example.com", "일반회원"));
        User admin = userRepository.saveAndFlush(createAdmin("admin@example.com", "운영자"));
        CouponPolicy couponPolicy = couponPolicyRepository.saveAndFlush(createOpenPolicy(admin));

        issuedCouponRepository.saveAndFlush(IssuedCoupon.issue(
                couponPolicy,
                user,
                LocalDateTime.of(2026, 3, 15, 9, 0)
        ));

        var userSession = login("user@example.com", "Password12!");
        var csrfSession = fetchCsrfSession(userSession);

        mockMvc.perform(post("/api/v1/coupons/{couponPolicyId}/issue", couponPolicy.getId())
                        .cookie(userSession)
                        .contentType(MediaType.APPLICATION_JSON)
                        .header(csrfSession.headerName(), csrfSession.token()))
                .andExpect(status().isConflict())
                .andExpect(jsonPath("$.code").value("COUPON_ALREADY_ISSUED"));

        assertThat(issuedCouponRepository.findAll()).hasSize(1);
    }

    @Test
    @DisplayName("SELLER는 쿠폰 발급 API에 접근할 수 없다")
    void sellerCannotIssueCoupon() throws Exception {
        userRepository.saveAndFlush(createSeller("seller@example.com", "판매자"));
        User admin = userRepository.saveAndFlush(createAdmin("admin@example.com", "운영자"));
        CouponPolicy couponPolicy = couponPolicyRepository.saveAndFlush(createOpenPolicy(admin));

        var sellerSession = login("seller@example.com", "Password12!");
        var csrfSession = fetchCsrfSession(sellerSession);

        mockMvc.perform(post("/api/v1/coupons/{couponPolicyId}/issue", couponPolicy.getId())
                        .cookie(sellerSession)
                        .contentType(MediaType.APPLICATION_JSON)
                        .header(csrfSession.headerName(), csrfSession.token()))
                .andExpect(status().isForbidden())
                .andExpect(jsonPath("$.code").value("AUTH_FORBIDDEN"));
    }

    @Test
    @DisplayName("ADMIN은 보유 쿠폰 조회 API에 접근할 수 없다")
    void adminCannotGetMyCoupons() throws Exception {
        userRepository.saveAndFlush(createAdmin("admin@example.com", "운영자"));

        var adminSession = login("admin@example.com", "Password12!");

        mockMvc.perform(get("/api/v1/coupons/me")
                        .cookie(adminSession))
                .andExpect(status().isForbidden())
                .andExpect(jsonPath("$.code").value("AUTH_FORBIDDEN"));
    }

    @Test
    @DisplayName("미인증 요청은 쿠폰 발급 API에서 AUTH_UNAUTHORIZED를 반환한다")
    void unauthenticatedIssueRequestIsRejected() throws Exception {
        User admin = userRepository.saveAndFlush(createAdmin("admin@example.com", "운영자"));
        CouponPolicy couponPolicy = couponPolicyRepository.saveAndFlush(createOpenPolicy(admin));

        var csrfSession = fetchCsrfSession();

        mockMvc.perform(post("/api/v1/coupons/{couponPolicyId}/issue", couponPolicy.getId())
                        .cookie(csrfSession.sessionCookie())
                        .contentType(MediaType.APPLICATION_JSON)
                        .header(csrfSession.headerName(), csrfSession.token()))
                .andExpect(status().isUnauthorized())
                .andExpect(jsonPath("$.code").value("AUTH_UNAUTHORIZED"));
    }

    @Test
    @DisplayName("CSRF 토큰 없이 쿠폰 발급을 요청하면 AUTH_CSRF_INVALID를 반환한다")
    void issueCouponRequiresCsrf() throws Exception {
        User admin = userRepository.saveAndFlush(createAdmin("admin@example.com", "운영자"));
        userRepository.saveAndFlush(createUser("user@example.com", "일반회원"));
        CouponPolicy couponPolicy = couponPolicyRepository.saveAndFlush(createOpenPolicy(admin));

        var userSession = login("user@example.com", "Password12!");

        mockMvc.perform(post("/api/v1/coupons/{couponPolicyId}/issue", couponPolicy.getId())
                        .cookie(userSession)
                        .contentType(MediaType.APPLICATION_JSON))
                .andExpect(status().isForbidden())
                .andExpect(jsonPath("$.code").value("AUTH_CSRF_INVALID"));
    }

    private User createUser(String email, String nickname) {
        return User.createUser(email, passwordEncoder.encode("Password12!"), nickname);
    }

    private User createSeller(String email, String nickname) {
        User seller = User.createUser(email, passwordEncoder.encode("Password12!"), nickname);
        seller.approveSeller(LocalDateTime.now(clock));
        return seller;
    }

    private User createAdmin(String email, String nickname) {
        return User.createAdmin(email, passwordEncoder.encode("Password12!"), nickname);
    }

    private CouponPolicy createOpenPolicy(User admin) {
        return createOpenPolicy(admin, "봄맞이 할인", 3000L, 20000L, 100);
    }

    private CouponPolicy createOpenPolicy(
            User admin,
            String name,
            Long discountAmount,
            Long minimumOrderAmount,
            Integer totalQuantity
    ) {
        CouponPolicy couponPolicy = CouponPolicy.create(
                name,
                discountAmount,
                minimumOrderAmount,
                totalQuantity,
                LocalDateTime.of(2026, 3, 10, 0, 0),
                LocalDateTime.of(2026, 3, 31, 23, 59),
                admin
        );
        couponPolicy.open();
        return couponPolicy;
    }

    private jakarta.servlet.http.Cookie login(String email, String password) throws Exception {
        CsrfSession csrfSession = fetchCsrfSession();

        MvcResult result = mockMvc.perform(post("/api/v1/auth/login")
                        .contentType(MediaType.APPLICATION_JSON)
                        .cookie(csrfSession.sessionCookie())
                        .header(csrfSession.headerName(), csrfSession.token())
                        .content(objectMapper.writeValueAsString(
                                new LoginRequest(email, password)
                        )))
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
        MvcResult result = mockMvc.perform(get("/api/v1/csrf")
                        .cookie(sessionCookie))
                .andExpect(status().isOk())
                .andReturn();
        return toCsrfSession(result, sessionCookie);
    }

    private CsrfSession toCsrfSession(MvcResult result, jakarta.servlet.http.Cookie fallbackCookie) throws Exception {
        JsonNode data = objectMapper.readTree(result.getResponse().getContentAsString()).get("data");
        var sessionCookie = result.getResponse().getCookie("SESSION");
        if (sessionCookie == null) {
            sessionCookie = fallbackCookie;
        }
        assertThat(sessionCookie).isNotNull();
        return new CsrfSession(
                sessionCookie,
                data.get("headerName").asText(),
                data.get("token").asText()
        );
    }

    private record LoginRequest(
            String email,
            String password
    ) {
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
            return new MutableClock(Instant.parse("2026-03-15T10:00:00Z"), ZoneId.of("UTC"));
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
