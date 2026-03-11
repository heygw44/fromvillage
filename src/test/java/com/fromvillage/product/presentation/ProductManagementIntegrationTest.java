package com.fromvillage.product.presentation;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.fromvillage.cart.infrastructure.CartJpaRepository;
import com.fromvillage.product.domain.Product;
import com.fromvillage.product.domain.ProductCategory;
import com.fromvillage.support.TestContainersConfig;
import com.fromvillage.user.domain.User;
import com.fromvillage.user.infrastructure.UserJpaRepository;
import jakarta.servlet.http.Cookie;
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

import static org.assertj.core.api.Assertions.assertThat;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.delete;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.put;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

@SpringBootTest
@AutoConfigureMockMvc
@ActiveProfiles("test")
@Import({
                TestContainersConfig.class,
                ProductManagementIntegrationTest.TestClockConfig.class
})
class ProductManagementIntegrationTest {

        @Autowired
        private MockMvc mockMvc;

        @Autowired
        private ObjectMapper objectMapper;

        @Autowired
        private UserJpaRepository userRepository;

        @Autowired
        private com.fromvillage.product.infrastructure.ProductJpaRepository productRepository;

        @Autowired
        private CartJpaRepository cartRepository;

        @Autowired
        private PasswordEncoder passwordEncoder;

        @Autowired
        private MutableClock clock;

        @BeforeEach
        void setUp() {
                cartRepository.deleteAll();
                productRepository.deleteAll();
                userRepository.deleteAll();
                clock.reset(Instant.parse("2026-03-10T00:00:00Z"));
        }

        @Test
        @DisplayName("SELLER는 상품을 등록할 수 있다")
        void sellerCanCreateProduct() throws Exception {
                userRepository.saveAndFlush(createSeller("seller@example.com", "판매자"));

                Cookie sellerSession = login("seller@example.com", "Password12!");
                CsrfSession csrfSession = fetchCsrfSession(sellerSession);

                mockMvc.perform(post("/api/v1/products")
                                .cookie(sellerSession)
                                .contentType(MediaType.APPLICATION_JSON)
                                .header(csrfSession.headerName(), csrfSession.token())
                                .content(objectMapper.writeValueAsString(validProductRequest())))
                                .andExpect(status().isCreated())
                                .andExpect(jsonPath("$.success").value(true))
                                .andExpect(jsonPath("$.data.productId").isNumber())
                                .andExpect(jsonPath("$.data.name").value("유기농 감자 5kg"))
                                .andExpect(jsonPath("$.data.status").value("ON_SALE"))
                                .andExpect(jsonPath("$.data.deletedAt").isEmpty());

                assertThat(productRepository.findAll()).hasSize(1);
                assertThat(productRepository.findAll().get(0).getName()).isEqualTo("유기농 감자 5kg");
        }

        @Test
        @DisplayName("SELLER는 본인 상품을 수정할 수 있다")
        void sellerCanUpdateOwnProduct() throws Exception {
                User seller = userRepository.saveAndFlush(createSeller("seller@example.com", "판매자"));
                Product product = productRepository.saveAndFlush(Product.create(
                                seller,
                                "유기농 감자 5kg",
                                "해남 햇감자",
                                ProductCategory.AGRICULTURE,
                                22000L,
                                8,
                                "https://cdn.example.com/potato.jpg"));

                Cookie sellerSession = login("seller@example.com", "Password12!");
                CsrfSession csrfSession = fetchCsrfSession(sellerSession);

                mockMvc.perform(put("/api/v1/products/{productId}", product.getId())
                                .cookie(sellerSession)
                                .contentType(MediaType.APPLICATION_JSON)
                                .header(csrfSession.headerName(), csrfSession.token())
                                .content(objectMapper.writeValueAsString(Map.of(
                                                "name", "완도 활전복 1kg",
                                                "description", "완도 산지 직송",
                                                "category", "FISHERY",
                                                "price", 45000,
                                                "stockQuantity", 0,
                                                "imageUrl", "https://cdn.example.com/abalone.jpg"))))
                                .andExpect(status().isOk())
                                .andExpect(jsonPath("$.data.productId").value(product.getId()))
                                .andExpect(jsonPath("$.data.name").value("완도 활전복 1kg"))
                                .andExpect(jsonPath("$.data.category").value("FISHERY"))
                                .andExpect(jsonPath("$.data.status").value("SOLD_OUT"));
        }

        @Test
        @DisplayName("SELLER는 본인 상품을 soft delete할 수 있다")
        void sellerCanSoftDeleteOwnProduct() throws Exception {
                User seller = userRepository.saveAndFlush(createSeller("seller@example.com", "판매자"));
                Product product = productRepository.saveAndFlush(Product.create(
                                seller,
                                "유기농 감자 5kg",
                                "해남 햇감자",
                                ProductCategory.AGRICULTURE,
                                22000L,
                                8,
                                "https://cdn.example.com/potato.jpg"));

                Cookie sellerSession = login("seller@example.com", "Password12!");
                CsrfSession csrfSession = fetchCsrfSession(sellerSession);

                mockMvc.perform(delete("/api/v1/products/{productId}", product.getId())
                                .cookie(sellerSession)
                                .header(csrfSession.headerName(), csrfSession.token()))
                                .andExpect(status().isOk())
                                .andExpect(jsonPath("$.success").value(true))
                                .andExpect(jsonPath("$.data").isEmpty());

                Product deletedProduct = productRepository.findById(product.getId()).orElseThrow();
                assertThat(deletedProduct.getDeletedAt()).isEqualTo(LocalDateTime.of(2026, 3, 10, 0, 0));
        }

        @Test
        @DisplayName("USER는 상품 등록 API에 접근할 수 없다")
        void userCannotCreateProduct() throws Exception {
                userRepository.saveAndFlush(User.createUser(
                                "user@example.com",
                                passwordEncoder.encode("Password12!"),
                                "일반회원"));

                Cookie userSession = login("user@example.com", "Password12!");
                CsrfSession csrfSession = fetchCsrfSession(userSession);

                mockMvc.perform(post("/api/v1/products")
                                .cookie(userSession)
                                .contentType(MediaType.APPLICATION_JSON)
                                .header(csrfSession.headerName(), csrfSession.token())
                                .content(objectMapper.writeValueAsString(validProductRequest())))
                                .andExpect(status().isForbidden())
                                .andExpect(jsonPath("$.code").value("AUTH_FORBIDDEN"));
        }

        @Test
        @DisplayName("ADMIN은 상품 수정 API에 접근할 수 없다")
        void adminCannotUpdateProduct() throws Exception {
                User seller = userRepository.saveAndFlush(createSeller("seller@example.com", "판매자"));
                Product product = productRepository.saveAndFlush(Product.create(
                                seller,
                                "유기농 감자 5kg",
                                "해남 햇감자",
                                ProductCategory.AGRICULTURE,
                                22000L,
                                8,
                                "https://cdn.example.com/potato.jpg"));
                userRepository.saveAndFlush(User.createAdmin(
                                "admin@example.com",
                                passwordEncoder.encode("Password12!"),
                                "운영자"));

                Cookie adminSession = login("admin@example.com", "Password12!");
                CsrfSession csrfSession = fetchCsrfSession(adminSession);

                mockMvc.perform(put("/api/v1/products/{productId}", product.getId())
                                .cookie(adminSession)
                                .contentType(MediaType.APPLICATION_JSON)
                                .header(csrfSession.headerName(), csrfSession.token())
                                .content(objectMapper.writeValueAsString(validProductRequest())))
                                .andExpect(status().isForbidden())
                                .andExpect(jsonPath("$.code").value("AUTH_FORBIDDEN"));
        }

        @Test
        @DisplayName("미인증 사용자는 상품 삭제 API에 접근할 수 없다")
        void unauthenticatedDeleteIsRejected() throws Exception {
                CsrfSession csrfSession = fetchCsrfSession();

                mockMvc.perform(delete("/api/v1/products/{productId}", 1L)
                                .cookie(csrfSession.sessionCookie())
                                .header(csrfSession.headerName(), csrfSession.token()))
                                .andExpect(status().isUnauthorized())
                                .andExpect(jsonPath("$.code").value("AUTH_UNAUTHORIZED"));
        }

        @Test
        @DisplayName("상품 관리 API는 CSRF 토큰이 없으면 거절된다")
        void createProductRequiresCsrf() throws Exception {
                userRepository.saveAndFlush(createSeller("seller@example.com", "판매자"));

                Cookie sellerSession = login("seller@example.com", "Password12!");

                mockMvc.perform(post("/api/v1/products")
                                .cookie(sellerSession)
                                .contentType(MediaType.APPLICATION_JSON)
                                .content(objectMapper.writeValueAsString(validProductRequest())))
                                .andExpect(status().isForbidden())
                                .andExpect(jsonPath("$.code").value("AUTH_CSRF_INVALID"));
        }

        @Test
        @DisplayName("http 이미지 URL로는 상품을 등록할 수 없다")
        void createProductRejectsNonHttpsImageUrl() throws Exception {
                userRepository.saveAndFlush(createSeller("seller@example.com", "판매자"));

                Cookie sellerSession = login("seller@example.com", "Password12!");
                CsrfSession csrfSession = fetchCsrfSession(sellerSession);

                mockMvc.perform(post("/api/v1/products")
                                .cookie(sellerSession)
                                .contentType(MediaType.APPLICATION_JSON)
                                .header(csrfSession.headerName(), csrfSession.token())
                                .content(objectMapper.writeValueAsString(Map.of(
                                                "name", "유기농 감자 5kg",
                                                "description", "해남 햇감자",
                                                "category", "AGRICULTURE",
                                                "price", 22000,
                                                "stockQuantity", 8,
                                                "imageUrl", "http://cdn.example.com/potato.jpg"))))
                                .andExpect(status().isBadRequest())
                                .andExpect(jsonPath("$.code").value("PRODUCT_IMAGE_URL_INVALID"));
        }

        @Test
        @DisplayName("타인 상품 수정은 거절된다")
        void updateOtherSellersProductIsRejected() throws Exception {
                User owner = userRepository.saveAndFlush(createSeller("owner@example.com", "원주인"));
                userRepository.saveAndFlush(createSeller("seller@example.com", "판매자"));
                Product product = productRepository.saveAndFlush(Product.create(
                                owner,
                                "유기농 감자 5kg",
                                "해남 햇감자",
                                ProductCategory.AGRICULTURE,
                                22000L,
                                8,
                                "https://cdn.example.com/potato.jpg"));

                Cookie sellerSession = login("seller@example.com", "Password12!");
                CsrfSession csrfSession = fetchCsrfSession(sellerSession);

                mockMvc.perform(put("/api/v1/products/{productId}", product.getId())
                                .cookie(sellerSession)
                                .contentType(MediaType.APPLICATION_JSON)
                                .header(csrfSession.headerName(), csrfSession.token())
                                .content(objectMapper.writeValueAsString(validProductRequest())))
                                .andExpect(status().isForbidden())
                                .andExpect(jsonPath("$.code").value("AUTH_FORBIDDEN"));
        }

        @Test
        @DisplayName("타인 상품 삭제는 거절된다")
        void deleteOtherSellersProductIsRejected() throws Exception {
                User owner = userRepository.saveAndFlush(createSeller("owner@example.com", "원주인"));
                userRepository.saveAndFlush(createSeller("seller@example.com", "판매자"));
                Product product = productRepository.saveAndFlush(Product.create(
                                owner,
                                "유기농 감자 5kg",
                                "해남 햇감자",
                                ProductCategory.AGRICULTURE,
                                22000L,
                                8,
                                "https://cdn.example.com/potato.jpg"));

                Cookie sellerSession = login("seller@example.com", "Password12!");
                CsrfSession csrfSession = fetchCsrfSession(sellerSession);

                mockMvc.perform(delete("/api/v1/products/{productId}", product.getId())
                                .cookie(sellerSession)
                                .header(csrfSession.headerName(), csrfSession.token()))
                                .andExpect(status().isForbidden())
                                .andExpect(jsonPath("$.code").value("AUTH_FORBIDDEN"));
        }

        @Test
        @DisplayName("상품 가격이 0 이하이면 요청 단계에서 거절된다")
        void createProductRejectsNonPositivePriceAtRequestLayer() throws Exception {
                userRepository.saveAndFlush(createSeller("seller@example.com", "판매자"));

                Cookie sellerSession = login("seller@example.com", "Password12!");
                CsrfSession csrfSession = fetchCsrfSession(sellerSession);

                mockMvc.perform(post("/api/v1/products")
                                .cookie(sellerSession)
                                .contentType(MediaType.APPLICATION_JSON)
                                .header(csrfSession.headerName(), csrfSession.token())
                                .content(objectMapper.writeValueAsString(Map.of(
                                                "name", "유기농 감자 5kg",
                                                "description", "해남 햇감자",
                                                "category", "AGRICULTURE",
                                                "price", 0,
                                                "stockQuantity", 8,
                                                "imageUrl", "https://cdn.example.com/potato.jpg"))))
                                .andExpect(status().isBadRequest())
                                .andExpect(jsonPath("$.code").value("VALIDATION_ERROR"))
                                .andExpect(jsonPath("$.errors[0].reason").value("상품 가격은 1원 이상이어야 합니다."));
        }

        @Test
        @DisplayName("재고 수량이 0 미만이면 요청 단계에서 거절된다")
        void createProductRejectsNegativeStockQuantityAtRequestLayer() throws Exception {
                userRepository.saveAndFlush(createSeller("seller@example.com", "판매자"));

                Cookie sellerSession = login("seller@example.com", "Password12!");
                CsrfSession csrfSession = fetchCsrfSession(sellerSession);

                mockMvc.perform(post("/api/v1/products")
                                .cookie(sellerSession)
                                .contentType(MediaType.APPLICATION_JSON)
                                .header(csrfSession.headerName(), csrfSession.token())
                                .content(objectMapper.writeValueAsString(Map.of(
                                                "name", "유기농 감자 5kg",
                                                "description", "해남 햇감자",
                                                "category", "AGRICULTURE",
                                                "price", 22000,
                                                "stockQuantity", -1,
                                                "imageUrl", "https://cdn.example.com/potato.jpg"))))
                                .andExpect(status().isBadRequest())
                                .andExpect(jsonPath("$.code").value("VALIDATION_ERROR"))
                                .andExpect(jsonPath("$.errors[0].reason").value("재고 수량은 0 이상이어야 합니다."));
        }

        @Test
        @DisplayName("잘못된 카테고리 값은 요청 단계에서 거절된다")
        void createProductRejectsInvalidCategory() throws Exception {
                userRepository.saveAndFlush(createSeller("seller@example.com", "판매자"));

                Cookie sellerSession = login("seller@example.com", "Password12!");
                CsrfSession csrfSession = fetchCsrfSession(sellerSession);

                mockMvc.perform(post("/api/v1/products")
                                .cookie(sellerSession)
                                .contentType(MediaType.APPLICATION_JSON)
                                .header(csrfSession.headerName(), csrfSession.token())
                                .content(objectMapper.writeValueAsString(Map.of(
                                                "name", "유기농 감자 5kg",
                                                "description", "해남 햇감자",
                                                "category", "FRUIT",
                                                "price", 22000,
                                                "stockQuantity", 8,
                                                "imageUrl", "https://cdn.example.com/potato.jpg"))))
                                .andExpect(status().isBadRequest())
                                .andExpect(jsonPath("$.code").value("VALIDATION_ERROR"))
                                .andExpect(jsonPath("$.errors[0].reason").value("요청 본문을 읽을 수 없습니다."));
        }

        @Test
        @DisplayName("SELLER는 본인 상품 목록을 조회할 수 있고 soft delete 상품도 포함된다")
        void sellerCanGetOwnProductsIncludingDeleted() throws Exception {
                User seller = userRepository.saveAndFlush(createSeller("seller@example.com", "판매자"));
                User otherSeller = userRepository.saveAndFlush(createSeller("other@example.com", "다른판매자"));

                Product activeProduct = productRepository.saveAndFlush(Product.create(
                                seller,
                                "유기농 감자 5kg",
                                "해남 햇감자",
                                ProductCategory.AGRICULTURE,
                                22000L,
                                8,
                                "https://cdn.example.com/potato.jpg"));
                Product deletedProduct = productRepository.saveAndFlush(Product.create(
                                seller,
                                "완도 활전복 1kg",
                                "완도 산지 직송",
                                ProductCategory.FISHERY,
                                45000L,
                                0,
                                "https://cdn.example.com/abalone.jpg"));
                deletedProduct.softDelete(LocalDateTime.of(2026, 3, 10, 0, 0));
                productRepository.saveAndFlush(deletedProduct);

                productRepository.saveAndFlush(Product.create(
                                otherSeller,
                                "남해 멸치",
                                "건멸치",
                                ProductCategory.FISHERY,
                                15000L,
                                3,
                                "https://cdn.example.com/anchovy.jpg"));

                Cookie sellerSession = login("seller@example.com", "Password12!");

                mockMvc.perform(get("/api/v1/seller/products")
                                .cookie(sellerSession)
                                .param("page", "0")
                                .param("size", "10"))
                                .andExpect(status().isOk())
                                .andExpect(jsonPath("$.success").value(true))
                                .andExpect(jsonPath("$.data.content.length()").value(2))
                                .andExpect(jsonPath("$.data.content[0].productId").value(deletedProduct.getId()))
                                .andExpect(jsonPath("$.data.content[0].deletedAt").value("2026-03-10T00:00:00"))
                                .andExpect(jsonPath("$.data.content[1].productId").value(activeProduct.getId()))
                                .andExpect(jsonPath("$.data.content[1].deletedAt").isEmpty())
                                .andExpect(jsonPath("$.data.page").value(0))
                                .andExpect(jsonPath("$.data.size").value(10))
                                .andExpect(jsonPath("$.data.totalElements").value(2))
                                .andExpect(jsonPath("$.data.totalPages").value(1))
                                .andExpect(jsonPath("$.data.hasNext").value(false));
        }

        @Test
        @DisplayName("USER는 SELLER 상품 목록 조회 API에 접근할 수 없다")
        void userCannotGetSellerProducts() throws Exception {
                userRepository.saveAndFlush(User.createUser(
                                "user@example.com",
                                passwordEncoder.encode("Password12!"),
                                "일반회원"));

                Cookie userSession = login("user@example.com", "Password12!");

                mockMvc.perform(get("/api/v1/seller/products")
                                .cookie(userSession))
                                .andExpect(status().isForbidden())
                                .andExpect(jsonPath("$.code").value("AUTH_FORBIDDEN"));
        }

        @Test
        @DisplayName("ADMIN은 SELLER 상품 목록 조회 API에 접근할 수 없다")
        void adminCannotGetSellerProducts() throws Exception {
                userRepository.saveAndFlush(User.createAdmin(
                                "admin@example.com",
                                passwordEncoder.encode("Password12!"),
                                "운영자"));

                Cookie adminSession = login("admin@example.com", "Password12!");

                mockMvc.perform(get("/api/v1/seller/products")
                                .cookie(adminSession))
                                .andExpect(status().isForbidden())
                                .andExpect(jsonPath("$.code").value("AUTH_FORBIDDEN"));
        }

        @Test
        @DisplayName("미인증 사용자는 SELLER 상품 목록 조회 API에 접근할 수 없다")
        void unauthenticatedSellerProductsRequestIsRejected() throws Exception {
                mockMvc.perform(get("/api/v1/seller/products"))
                                .andExpect(status().isUnauthorized())
                                .andExpect(jsonPath("$.code").value("AUTH_UNAUTHORIZED"));
        }

        private User createSeller(String email, String nickname) {
                User seller = User.createUser(email, passwordEncoder.encode("Password12!"), nickname);
                seller.approveSeller(LocalDateTime.of(2026, 3, 9, 0, 0));
                return seller;
        }

        private Map<String, Object> validProductRequest() {
                return Map.of(
                                "name", "유기농 감자 5kg",
                                "description", "해남 햇감자",
                                "category", "AGRICULTURE",
                                "price", 22000,
                                "stockQuantity", 8,
                                "imageUrl", "https://cdn.example.com/potato.jpg");
        }

        private Cookie login(String email, String password) throws Exception {
                CsrfSession csrfSession = fetchCsrfSession();

                MvcResult result = mockMvc.perform(post("/api/v1/auth/login")
                                .cookie(csrfSession.sessionCookie())
                                .contentType(MediaType.APPLICATION_JSON)
                                .header(csrfSession.headerName(), csrfSession.token())
                                .content(objectMapper.writeValueAsString(Map.of(
                                                "email", email,
                                                "password", password))))
                                .andExpect(status().isOk())
                                .andReturn();

                Cookie sessionCookie = result.getResponse().getCookie("SESSION");
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

        private CsrfSession fetchCsrfSession(Cookie sessionCookie) throws Exception {
                MvcResult result = mockMvc.perform(get("/api/v1/csrf").cookie(sessionCookie))
                                .andExpect(status().isOk())
                                .andReturn();

                return toCsrfSession(result, sessionCookie);
        }

        private CsrfSession toCsrfSession(MvcResult result, Cookie fallbackCookie) throws Exception {
                JsonNode root = objectMapper.readTree(result.getResponse().getContentAsString());
                Cookie sessionCookie = result.getResponse().getCookie("SESSION");
                if (sessionCookie == null) {
                        sessionCookie = fallbackCookie;
                }
                assertThat(sessionCookie).isNotNull();
                return new CsrfSession(
                                sessionCookie,
                                root.path("data").path("headerName").asText(),
                                root.path("data").path("token").asText());
        }

        private record CsrfSession(
                        Cookie sessionCookie,
                        String headerName,
                        String token) {
        }

        @TestConfiguration(proxyBeanMethods = false)
        static class TestClockConfig {

                @Bean
                @Primary
                MutableClock mutableClock() {
                        return new MutableClock(Instant.parse("2026-03-10T00:00:00Z"), ZoneId.of("UTC"));
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
