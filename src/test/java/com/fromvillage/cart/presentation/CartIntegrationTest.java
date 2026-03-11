package com.fromvillage.cart.presentation;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.fromvillage.cart.domain.CartItem;
import com.fromvillage.cart.infrastructure.CartJpaRepository;
import com.fromvillage.product.domain.Product;
import com.fromvillage.product.domain.ProductCategory;
import com.fromvillage.product.infrastructure.ProductJpaRepository;
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
import org.springframework.context.annotation.Import;
import org.springframework.http.MediaType;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.MvcResult;


import java.time.LocalDateTime;
import java.util.Map;

import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.delete;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.patch;

@SpringBootTest
@AutoConfigureMockMvc
@ActiveProfiles("test")
@Import(TestContainersConfig.class)
class CartIntegrationTest {

    @Autowired
    private MockMvc mockMvc;

    @Autowired
    private ObjectMapper objectMapper;

    @Autowired
    private UserJpaRepository userRepository;

    @Autowired
    private ProductJpaRepository productRepository;

    @Autowired
    private CartJpaRepository cartRepository;

    @Autowired
    private PasswordEncoder passwordEncoder;

    @BeforeEach
    void setUp() {
        cartRepository.deleteAll();
        productRepository.deleteAll();
        userRepository.deleteAll();
    }

    @Test
    @DisplayName("USER는 본인 장바구니의 주문 가능 항목만 조회할 수 있다")
    void userCanGetOwnActiveCartItems() throws Exception {
        User seller = userRepository.saveAndFlush(createSeller("seller@example.com", "판매자"));
        User user = userRepository.saveAndFlush(User.createUser(
                "user@example.com",
                passwordEncoder.encode("Password12!"),
                "구매자"
        ));

        Product activeProduct = productRepository.saveAndFlush(Product.create(
                seller,
                "유기농 감자 5kg",
                "해남 햇감자",
                ProductCategory.AGRICULTURE,
                22000L,
                10,
                "https://cdn.example.com/potato.jpg"
        ));

        Product deletedProduct = productRepository.saveAndFlush(Product.create(
                seller,
                "손질 고등어",
                "당일 손질",
                ProductCategory.FISHERY,
                18000L,
                5,
                "https://cdn.example.com/mackerel.jpg"
        ));
        deletedProduct.softDelete(LocalDateTime.now());
        productRepository.saveAndFlush(deletedProduct);

        cartRepository.saveAndFlush(CartItem.create(user, activeProduct, 2));
        cartRepository.saveAndFlush(CartItem.create(user, deletedProduct, 1));

        Cookie userSession = login("user@example.com", "Password12!");

        mockMvc.perform(get("/api/v1/cart-items")
                        .cookie(userSession))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.success").value(true))
                .andExpect(jsonPath("$.code").value("SUCCESS"))
                .andExpect(jsonPath("$.data.items.length()").value(1))
                .andExpect(jsonPath("$.data.items[0].productId").value(activeProduct.getId()))
                .andExpect(jsonPath("$.data.items[0].productName").value("유기농 감자 5kg"))
                .andExpect(jsonPath("$.data.items[0].quantity").value(2))
                .andExpect(jsonPath("$.data.totalItemCount").value(1))
                .andExpect(jsonPath("$.data.totalQuantity").value(2))
                .andExpect(jsonPath("$.data.totalAmount").value(44000));
    }

    @Test
    @DisplayName("USER는 상품을 장바구니에 담을 수 있다")
    void userCanAddCartItem() throws Exception {
        User seller = userRepository.saveAndFlush(createSeller("seller@example.com", "판매자"));
        userRepository.saveAndFlush(User.createUser(
                "user@example.com",
                passwordEncoder.encode("Password12!"),
                "구매자"
        ));

        Product product = productRepository.saveAndFlush(Product.create(
                seller,
                "유기농 감자 5kg",
                "해남 햇감자",
                ProductCategory.AGRICULTURE,
                22000L,
                10,
                "https://cdn.example.com/potato.jpg"
        ));

        Cookie userSession = login("user@example.com", "Password12!");
        CsrfSession csrfSession = fetchCsrfSession(userSession);

        mockMvc.perform(post("/api/v1/cart-items")
                        .cookie(userSession)
                        .contentType(MediaType.APPLICATION_JSON)
                        .header(csrfSession.headerName(), csrfSession.token())
                        .content(objectMapper.writeValueAsString(Map.of(
                                "productId", product.getId(),
                                "quantity", 2
                        ))))
                .andExpect(status().isCreated())
                .andExpect(jsonPath("$.success").value(true))
                .andExpect(jsonPath("$.code").value("SUCCESS"))
                .andExpect(jsonPath("$.data.productId").value(product.getId()))
                .andExpect(jsonPath("$.data.productName").value("유기농 감자 5kg"))
                .andExpect(jsonPath("$.data.quantity").value(2))
                .andExpect(jsonPath("$.data.lineAmount").value(44000));

        mockMvc.perform(get("/api/v1/cart-items")
                        .cookie(userSession))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.data.items.length()").value(1))
                .andExpect(jsonPath("$.data.totalItemCount").value(1))
                .andExpect(jsonPath("$.data.totalQuantity").value(2))
                .andExpect(jsonPath("$.data.totalAmount").value(44000));
    }

    @Test
    @DisplayName("같은 상품을 다시 담으면 기존 장바구니 수량에 합산된다")
    void addCartItemMergesQuantityWhenSameProductAlreadyExists() throws Exception {
        User seller = userRepository.saveAndFlush(createSeller("seller@example.com", "판매자"));
        User user = userRepository.saveAndFlush(User.createUser(
                "user@example.com",
                passwordEncoder.encode("Password12!"),
                "구매자"
        ));

        Product product = productRepository.saveAndFlush(Product.create(
                seller,
                "유기농 감자 5kg",
                "해남 햇감자",
                ProductCategory.AGRICULTURE,
                22000L,
                10,
                "https://cdn.example.com/potato.jpg"
        ));

        cartRepository.saveAndFlush(CartItem.create(user, product, 2));

        Cookie userSession = login("user@example.com", "Password12!");
        CsrfSession csrfSession = fetchCsrfSession(userSession);

        mockMvc.perform(post("/api/v1/cart-items")
                        .cookie(userSession)
                        .contentType(MediaType.APPLICATION_JSON)
                        .header(csrfSession.headerName(), csrfSession.token())
                        .content(objectMapper.writeValueAsString(Map.of(
                                "productId", product.getId(),
                                "quantity", 3
                        ))))
                .andExpect(status().isCreated())
                .andExpect(jsonPath("$.success").value(true))
                .andExpect(jsonPath("$.code").value("SUCCESS"))
                .andExpect(jsonPath("$.data.productId").value(product.getId()))
                .andExpect(jsonPath("$.data.quantity").value(5))
                .andExpect(jsonPath("$.data.lineAmount").value(110000));

        mockMvc.perform(get("/api/v1/cart-items")
                        .cookie(userSession))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.data.items.length()").value(1))
                .andExpect(jsonPath("$.data.items[0].productId").value(product.getId()))
                .andExpect(jsonPath("$.data.items[0].quantity").value(5))
                .andExpect(jsonPath("$.data.totalItemCount").value(1))
                .andExpect(jsonPath("$.data.totalQuantity").value(5))
                .andExpect(jsonPath("$.data.totalAmount").value(110000));
    }

    @Test
    @DisplayName("USER는 본인 장바구니 항목 수량을 수정할 수 있다")
    void userCanUpdateCartItemQuantity() throws Exception {
        User seller = userRepository.saveAndFlush(createSeller("seller@example.com", "판매자"));
        User user = userRepository.saveAndFlush(User.createUser(
                "user@example.com",
                passwordEncoder.encode("Password12!"),
                "구매자"
        ));

        Product product = productRepository.saveAndFlush(Product.create(
                seller,
                "유기농 감자 5kg",
                "해남 햇감자",
                ProductCategory.AGRICULTURE,
                22000L,
                10,
                "https://cdn.example.com/potato.jpg"
        ));

        CartItem cartItem = cartRepository.saveAndFlush(CartItem.create(user, product, 2));

        Cookie userSession = login("user@example.com", "Password12!");
        CsrfSession csrfSession = fetchCsrfSession(userSession);

        mockMvc.perform(patch("/api/v1/cart-items/{cartItemId}", cartItem.getId())
                        .cookie(userSession)
                        .contentType(MediaType.APPLICATION_JSON)
                        .header(csrfSession.headerName(), csrfSession.token())
                        .content(objectMapper.writeValueAsString(Map.of(
                                "quantity", 5
                        ))))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.success").value(true))
                .andExpect(jsonPath("$.code").value("SUCCESS"))
                .andExpect(jsonPath("$.data.cartItemId").value(cartItem.getId()))
                .andExpect(jsonPath("$.data.productId").value(product.getId()))
                .andExpect(jsonPath("$.data.quantity").value(5))
                .andExpect(jsonPath("$.data.lineAmount").value(110000));

        mockMvc.perform(get("/api/v1/cart-items")
                        .cookie(userSession))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.data.items.length()").value(1))
                .andExpect(jsonPath("$.data.items[0].cartItemId").value(cartItem.getId()))
                .andExpect(jsonPath("$.data.items[0].quantity").value(5))
                .andExpect(jsonPath("$.data.totalItemCount").value(1))
                .andExpect(jsonPath("$.data.totalQuantity").value(5))
                .andExpect(jsonPath("$.data.totalAmount").value(110000));
    }

    @Test
    @DisplayName("장바구니 수량 수정 시 1 미만은 거절된다")
    void updateCartItemRejectsInvalidQuantity() throws Exception {
        User seller = userRepository.saveAndFlush(createSeller("seller@example.com", "판매자"));
        User user = userRepository.saveAndFlush(User.createUser(
                "user@example.com",
                passwordEncoder.encode("Password12!"),
                "구매자"
        ));

        Product product = productRepository.saveAndFlush(Product.create(
                seller,
                "유기농 감자 5kg",
                "해남 햇감자",
                ProductCategory.AGRICULTURE,
                22000L,
                10,
                "https://cdn.example.com/potato.jpg"
        ));

        CartItem cartItem = cartRepository.saveAndFlush(CartItem.create(user, product, 2));

        Cookie userSession = login("user@example.com", "Password12!");
        CsrfSession csrfSession = fetchCsrfSession(userSession);

        mockMvc.perform(patch("/api/v1/cart-items/{cartItemId}", cartItem.getId())
                        .cookie(userSession)
                        .contentType(MediaType.APPLICATION_JSON)
                        .header(csrfSession.headerName(), csrfSession.token())
                        .content(objectMapper.writeValueAsString(Map.of(
                                "quantity", 0
                        ))))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.success").value(false))
                .andExpect(jsonPath("$.code").value("CART_QUANTITY_INVALID"));
    }

    @Test
    @DisplayName("USER는 본인 장바구니 항목을 삭제할 수 있다")
    void userCanDeleteCartItem() throws Exception {
        User seller = userRepository.saveAndFlush(createSeller("seller@example.com", "판매자"));
        User user = userRepository.saveAndFlush(User.createUser(
                "user@example.com",
                passwordEncoder.encode("Password12!"),
                "구매자"
        ));

        Product product = productRepository.saveAndFlush(Product.create(
                seller,
                "유기농 감자 5kg",
                "해남 햇감자",
                ProductCategory.AGRICULTURE,
                22000L,
                10,
                "https://cdn.example.com/potato.jpg"
        ));

        CartItem cartItem = cartRepository.saveAndFlush(CartItem.create(user, product, 2));

        Cookie userSession = login("user@example.com", "Password12!");
        CsrfSession csrfSession = fetchCsrfSession(userSession);

        mockMvc.perform(delete("/api/v1/cart-items/{cartItemId}", cartItem.getId())
                        .cookie(userSession)
                        .header(csrfSession.headerName(), csrfSession.token()))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.success").value(true))
                .andExpect(jsonPath("$.code").value("SUCCESS"))
                .andExpect(jsonPath("$.data").isEmpty());

        mockMvc.perform(get("/api/v1/cart-items")
                        .cookie(userSession))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.data.items.length()").value(0))
                .andExpect(jsonPath("$.data.totalItemCount").value(0))
                .andExpect(jsonPath("$.data.totalQuantity").value(0))
                .andExpect(jsonPath("$.data.totalAmount").value(0));
    }

    @Test
    @DisplayName("SELLER는 장바구니 조회 API에 접근할 수 없다")
    void sellerCannotGetCartItems() throws Exception {
        userRepository.saveAndFlush(createSeller("seller@example.com", "판매자"));

        Cookie sellerSession = login("seller@example.com", "Password12!");

        mockMvc.perform(get("/api/v1/cart-items")
                        .cookie(sellerSession))
                .andExpect(status().isForbidden())
                .andExpect(jsonPath("$.code").value("AUTH_FORBIDDEN"));
    }

    @Test
    @DisplayName("ADMIN은 장바구니 담기 API에 접근할 수 없다")
    void adminCannotAddCartItem() throws Exception {
        User seller = userRepository.saveAndFlush(createSeller("seller@example.com", "판매자"));
        userRepository.saveAndFlush(User.createAdmin(
                "admin@example.com",
                passwordEncoder.encode("Password12!"),
                "운영자"
        ));

        Product product = productRepository.saveAndFlush(Product.create(
                seller,
                "유기농 감자 5kg",
                "해남 햇감자",
                ProductCategory.AGRICULTURE,
                22000L,
                10,
                "https://cdn.example.com/potato.jpg"
        ));

        Cookie adminSession = login("admin@example.com", "Password12!");
        CsrfSession csrfSession = fetchCsrfSession(adminSession);

        mockMvc.perform(post("/api/v1/cart-items")
                        .cookie(adminSession)
                        .contentType(MediaType.APPLICATION_JSON)
                        .header(csrfSession.headerName(), csrfSession.token())
                        .content(objectMapper.writeValueAsString(Map.of(
                                "productId", product.getId(),
                                "quantity", 2
                        ))))
                .andExpect(status().isForbidden())
                .andExpect(jsonPath("$.code").value("AUTH_FORBIDDEN"));
    }

    @Test
    @DisplayName("미인증 사용자는 장바구니 조회 API에 접근할 수 없다")
    void unauthenticatedUserCannotGetCartItems() throws Exception {
        mockMvc.perform(get("/api/v1/cart-items"))
                .andExpect(status().isUnauthorized())
                .andExpect(jsonPath("$.code").value("AUTH_UNAUTHORIZED"));
    }

    @Test
    @DisplayName("장바구니 담기는 CSRF 토큰이 없으면 거절된다")
    void addCartItemRequiresCsrf() throws Exception {
        User seller = userRepository.saveAndFlush(createSeller("seller@example.com", "판매자"));
        userRepository.saveAndFlush(User.createUser(
                "user@example.com",
                passwordEncoder.encode("Password12!"),
                "구매자"
        ));

        Product product = productRepository.saveAndFlush(Product.create(
                seller,
                "유기농 감자 5kg",
                "해남 햇감자",
                ProductCategory.AGRICULTURE,
                22000L,
                10,
                "https://cdn.example.com/potato.jpg"
        ));

        Cookie userSession = login("user@example.com", "Password12!");

        mockMvc.perform(post("/api/v1/cart-items")
                        .cookie(userSession)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(Map.of(
                                "productId", product.getId(),
                                "quantity", 2
                        ))))
                .andExpect(status().isForbidden())
                .andExpect(jsonPath("$.code").value("AUTH_CSRF_INVALID"));
    }

    @Test
    @DisplayName("장바구니 수량 수정은 CSRF 토큰이 없으면 거절된다")
    void updateCartItemRequiresCsrf() throws Exception {
        User seller = userRepository.saveAndFlush(createSeller("seller@example.com", "판매자"));
        User user = userRepository.saveAndFlush(User.createUser(
                "user@example.com",
                passwordEncoder.encode("Password12!"),
                "구매자"
        ));

        Product product = productRepository.saveAndFlush(Product.create(
                seller,
                "유기농 감자 5kg",
                "해남 햇감자",
                ProductCategory.AGRICULTURE,
                22000L,
                10,
                "https://cdn.example.com/potato.jpg"
        ));

        CartItem cartItem = cartRepository.saveAndFlush(CartItem.create(user, product, 2));
        Cookie userSession = login("user@example.com", "Password12!");

        mockMvc.perform(patch("/api/v1/cart-items/{cartItemId}", cartItem.getId())
                        .cookie(userSession)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(Map.of(
                                "quantity", 3
                        ))))
                .andExpect(status().isForbidden())
                .andExpect(jsonPath("$.code").value("AUTH_CSRF_INVALID"));
    }

    @Test
    @DisplayName("장바구니 삭제는 CSRF 토큰이 없으면 거절된다")
    void deleteCartItemRequiresCsrf() throws Exception {
        User seller = userRepository.saveAndFlush(createSeller("seller@example.com", "판매자"));
        User user = userRepository.saveAndFlush(User.createUser(
                "user@example.com",
                passwordEncoder.encode("Password12!"),
                "구매자"
        ));

        Product product = productRepository.saveAndFlush(Product.create(
                seller,
                "유기농 감자 5kg",
                "해남 햇감자",
                ProductCategory.AGRICULTURE,
                22000L,
                10,
                "https://cdn.example.com/potato.jpg"
        ));

        CartItem cartItem = cartRepository.saveAndFlush(CartItem.create(user, product, 2));
        Cookie userSession = login("user@example.com", "Password12!");

        mockMvc.perform(delete("/api/v1/cart-items/{cartItemId}", cartItem.getId())
                        .cookie(userSession))
                .andExpect(status().isForbidden())
                .andExpect(jsonPath("$.code").value("AUTH_CSRF_INVALID"));
    }

    @Test
    @DisplayName("다른 사용자의 장바구니 항목은 수정할 수 없다")
    void userCannotUpdateOtherUsersCartItem() throws Exception {
        User seller = userRepository.saveAndFlush(createSeller("seller@example.com", "판매자"));
        User owner = userRepository.saveAndFlush(User.createUser(
                "owner@example.com",
                passwordEncoder.encode("Password12!"),
                "원래주인"
        ));
        userRepository.saveAndFlush(User.createUser(
                "user@example.com",
                passwordEncoder.encode("Password12!"),
                "구매자"
        ));

        Product product = productRepository.saveAndFlush(Product.create(
                seller,
                "유기농 감자 5kg",
                "해남 햇감자",
                ProductCategory.AGRICULTURE,
                22000L,
                10,
                "https://cdn.example.com/potato.jpg"
        ));

        CartItem cartItem = cartRepository.saveAndFlush(CartItem.create(owner, product, 2));

        Cookie userSession = login("user@example.com", "Password12!");
        CsrfSession csrfSession = fetchCsrfSession(userSession);

        mockMvc.perform(patch("/api/v1/cart-items/{cartItemId}", cartItem.getId())
                        .cookie(userSession)
                        .contentType(MediaType.APPLICATION_JSON)
                        .header(csrfSession.headerName(), csrfSession.token())
                        .content(objectMapper.writeValueAsString(Map.of(
                                "quantity", 4
                        ))))
                .andExpect(status().isForbidden())
                .andExpect(jsonPath("$.code").value("AUTH_FORBIDDEN"));
    }

    @Test
    @DisplayName("다른 사용자의 장바구니 항목은 삭제할 수 없다")
    void userCannotDeleteOtherUsersCartItem() throws Exception {
        User seller = userRepository.saveAndFlush(createSeller("seller@example.com", "판매자"));
        User owner = userRepository.saveAndFlush(User.createUser(
                "owner@example.com",
                passwordEncoder.encode("Password12!"),
                "원래주인"
        ));
        userRepository.saveAndFlush(User.createUser(
                "user@example.com",
                passwordEncoder.encode("Password12!"),
                "구매자"
        ));

        Product product = productRepository.saveAndFlush(Product.create(
                seller,
                "유기농 감자 5kg",
                "해남 햇감자",
                ProductCategory.AGRICULTURE,
                22000L,
                10,
                "https://cdn.example.com/potato.jpg"
        ));

        CartItem cartItem = cartRepository.saveAndFlush(CartItem.create(owner, product, 2));

        Cookie userSession = login("user@example.com", "Password12!");
        CsrfSession csrfSession = fetchCsrfSession(userSession);

        mockMvc.perform(delete("/api/v1/cart-items/{cartItemId}", cartItem.getId())
                        .cookie(userSession)
                        .header(csrfSession.headerName(), csrfSession.token()))
                .andExpect(status().isForbidden())
                .andExpect(jsonPath("$.code").value("AUTH_FORBIDDEN"));
    }

    @Test
    @DisplayName("soft delete 상품은 장바구니에 담을 수 없다")
    void addCartItemRejectsDeletedProduct() throws Exception {
        User seller = userRepository.saveAndFlush(createSeller("seller@example.com", "판매자"));
        userRepository.saveAndFlush(User.createUser(
                "user@example.com",
                passwordEncoder.encode("Password12!"),
                "구매자"
        ));

        Product deletedProduct = productRepository.saveAndFlush(Product.create(
                seller,
                "손질 고등어",
                "당일 손질",
                ProductCategory.FISHERY,
                18000L,
                5,
                "https://cdn.example.com/mackerel.jpg"
        ));
        deletedProduct.softDelete(LocalDateTime.now());
        productRepository.saveAndFlush(deletedProduct);

        Cookie userSession = login("user@example.com", "Password12!");
        CsrfSession csrfSession = fetchCsrfSession(userSession);

        mockMvc.perform(post("/api/v1/cart-items")
                        .cookie(userSession)
                        .contentType(MediaType.APPLICATION_JSON)
                        .header(csrfSession.headerName(), csrfSession.token())
                        .content(objectMapper.writeValueAsString(Map.of(
                                "productId", deletedProduct.getId(),
                                "quantity", 2
                        ))))
                .andExpect(status().isConflict())
                .andExpect(jsonPath("$.code").value("CART_PRODUCT_UNAVAILABLE"));
    }

    @Test
    @DisplayName("품절 상품은 장바구니 수량을 수정할 수 없다")
    void updateCartItemRejectsSoldOutProduct() throws Exception {
        User seller = userRepository.saveAndFlush(createSeller("seller@example.com", "판매자"));
        User user = userRepository.saveAndFlush(User.createUser(
                "user@example.com",
                passwordEncoder.encode("Password12!"),
                "구매자"
        ));

        Product soldOutProduct = productRepository.saveAndFlush(Product.create(
                seller,
                "유기농 감자 5kg",
                "해남 햇감자",
                ProductCategory.AGRICULTURE,
                22000L,
                0,
                "https://cdn.example.com/potato.jpg"
        ));

        CartItem cartItem = cartRepository.saveAndFlush(CartItem.create(user, soldOutProduct, 1));

        Cookie userSession = login("user@example.com", "Password12!");
        CsrfSession csrfSession = fetchCsrfSession(userSession);

        mockMvc.perform(patch("/api/v1/cart-items/{cartItemId}", cartItem.getId())
                        .cookie(userSession)
                        .contentType(MediaType.APPLICATION_JSON)
                        .header(csrfSession.headerName(), csrfSession.token())
                        .content(objectMapper.writeValueAsString(Map.of(
                                "quantity", 3
                        ))))
                .andExpect(status().isConflict())
                .andExpect(jsonPath("$.code").value("CART_PRODUCT_UNAVAILABLE"));
    }





    private User createSeller(String email, String nickname) {
        User seller = User.createUser(
                email,
                passwordEncoder.encode("Password12!"),
                nickname
        );
        seller.approveSeller(LocalDateTime.of(2026, 3, 10, 0, 0));
        return seller;
    }

    private Cookie login(String email, String password) throws Exception {
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

        Cookie loginSession = result.getResponse().getCookie("SESSION");
        return loginSession != null ? loginSession : csrfSession.sessionCookie();
    }

    private CsrfSession fetchCsrfSession() throws Exception {
        MvcResult result = mockMvc.perform(get("/api/v1/csrf"))
                .andExpect(status().isOk())
                .andReturn();

        Cookie sessionCookie = result.getResponse().getCookie("SESSION");
        JsonNode data = objectMapper.readTree(result.getResponse().getContentAsString()).get("data");

        return new CsrfSession(
                sessionCookie,
                data.get("headerName").asText(),
                data.get("token").asText()
        );
    }

    private CsrfSession fetchCsrfSession(Cookie sessionCookie) throws Exception {
        MvcResult result = mockMvc.perform(get("/api/v1/csrf")
                        .cookie(sessionCookie))
                .andExpect(status().isOk())
                .andReturn();

        Cookie responseSessionCookie = result.getResponse().getCookie("SESSION");
        JsonNode data = objectMapper.readTree(result.getResponse().getContentAsString()).get("data");

        return new CsrfSession(
                responseSessionCookie != null ? responseSessionCookie : sessionCookie,
                data.get("headerName").asText(),
                data.get("token").asText()
        );
    }

    private record CsrfSession(
            Cookie sessionCookie,
            String headerName,
            String token
    ) {
    }

}
