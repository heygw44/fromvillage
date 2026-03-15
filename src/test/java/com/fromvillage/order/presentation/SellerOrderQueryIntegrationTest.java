package com.fromvillage.order.presentation;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.fromvillage.coupon.infrastructure.CouponPolicyJpaRepository;
import com.fromvillage.coupon.infrastructure.IssuedCouponJpaRepository;
import com.fromvillage.order.domain.CheckoutOrder;
import com.fromvillage.order.domain.OrderItem;
import com.fromvillage.order.domain.OrderStatus;
import com.fromvillage.order.domain.SellerOrder;
import com.fromvillage.order.infrastructure.CheckoutOrderJpaRepository;
import com.fromvillage.order.infrastructure.SellerOrderJpaRepository;
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
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.MvcResult;

import java.time.LocalDateTime;
import java.util.List;
import java.util.Map;

import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

@SpringBootTest
@AutoConfigureMockMvc
@ActiveProfiles("test")
@Import(TestContainersConfig.class)
class SellerOrderQueryIntegrationTest {

    @Autowired
    private MockMvc mockMvc;

    @Autowired
    private ObjectMapper objectMapper;

    @Autowired
    private UserJpaRepository userRepository;

    @Autowired
    private ProductJpaRepository productRepository;

    @Autowired
    private CheckoutOrderJpaRepository checkoutOrderRepository;

    @Autowired
    private SellerOrderJpaRepository sellerOrderRepository;

    @Autowired
    private PasswordEncoder passwordEncoder;

    @Autowired
    private IssuedCouponJpaRepository issuedCouponRepository;

    @Autowired
    private CouponPolicyJpaRepository couponPolicyRepository;

    @BeforeEach
    void setUp() {
        sellerOrderRepository.deleteAll();
        checkoutOrderRepository.deleteAll();
        issuedCouponRepository.deleteAll();
        couponPolicyRepository.deleteAll();
        productRepository.deleteAll();
        userRepository.deleteAll();
    }

    @Test
    @DisplayName("SELLER는 본인 판매자 주문 목록을 조회할 수 있다")
    void sellerCanGetOwnSellerOrders() throws Exception {
        User seller1 = userRepository.saveAndFlush(createSeller("seller1@example.com", "판매자1"));
        User seller2 = userRepository.saveAndFlush(createSeller("seller2@example.com", "판매자2"));
        User buyer = userRepository.saveAndFlush(User.createUser(
                "buyer@example.com",
                passwordEncoder.encode("Password12!"),
                "구매자"
        ));

        Product potato = productRepository.saveAndFlush(createProduct(seller1, "감자", 12000L));
        Product cabbage = productRepository.saveAndFlush(createProduct(seller2, "배추", 8000L));

        CheckoutOrder checkoutOrder = CheckoutOrder.create(
                buyer,
                List.of(
                        SellerOrder.create(seller1, List.of(OrderItem.create(potato, 2))),
                        SellerOrder.create(seller2, List.of(OrderItem.create(cabbage, 1)))
                )
        );
        checkoutOrder.complete(LocalDateTime.of(2026, 3, 14, 10, 0));
        checkoutOrderRepository.saveAndFlush(checkoutOrder);

        Cookie sellerSession = login("seller1@example.com", "Password12!");

        mockMvc.perform(get("/api/v1/seller-orders")
                        .cookie(sellerSession))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.code").value("SUCCESS"))
                .andExpect(jsonPath("$.data.content.length()").value(1))
                .andExpect(jsonPath("$.data.content[0].orderNumber").value(checkoutOrder.getOrderNumber()))
                .andExpect(jsonPath("$.data.content[0].buyerNickname").value("구매자"))
                .andExpect(jsonPath("$.data.content[0].status").value("COMPLETED"))
                .andExpect(jsonPath("$.data.content[0].totalAmount").value(24000))
                .andExpect(jsonPath("$.data.content[0].sellerOrderId").isNumber());
    }

    @Test
    @DisplayName("SELLER는 본인 판매자 주문 상세를 조회할 수 있다")
    void sellerCanGetOwnSellerOrderDetail() throws Exception {
        User seller = userRepository.saveAndFlush(createSeller("seller@example.com", "판매자"));
        User buyer = userRepository.saveAndFlush(User.createUser(
                "buyer@example.com",
                passwordEncoder.encode("Password12!"),
                "구매자"
        ));

        Product potato = productRepository.saveAndFlush(createProduct(seller, "감자", 12000L));
        Product cabbage = productRepository.saveAndFlush(createProduct(seller, "배추", 8000L));

        CheckoutOrder checkoutOrder = CheckoutOrder.create(
                buyer,
                List.of(
                        SellerOrder.create(seller, List.of(
                                OrderItem.create(potato, 2),
                                OrderItem.create(cabbage, 1)
                        ))
                )
        );
        checkoutOrder.complete(LocalDateTime.of(2026, 3, 14, 11, 0));
        CheckoutOrder savedOrder = checkoutOrderRepository.saveAndFlush(checkoutOrder);
        Long sellerOrderId = sellerOrderRepository.findAllByCheckoutOrderIdWithItems(savedOrder.getId()).getFirst().getId();

        Cookie sellerSession = login("seller@example.com", "Password12!");

        mockMvc.perform(get("/api/v1/seller-orders/{sellerOrderId}", sellerOrderId)
                        .cookie(sellerSession))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.code").value("SUCCESS"))
                .andExpect(jsonPath("$.data.sellerOrderId").value(sellerOrderId))
                .andExpect(jsonPath("$.data.orderNumber").value(savedOrder.getOrderNumber()))
                .andExpect(jsonPath("$.data.buyerNickname").value("구매자"))
                .andExpect(jsonPath("$.data.status").value("COMPLETED"))
                .andExpect(jsonPath("$.data.orderItems.length()").value(2))
                .andExpect(jsonPath("$.data.orderItems[0].productNameSnapshot").exists())
                .andExpect(jsonPath("$.data.orderItems[0].productPriceSnapshot").exists())
                .andExpect(jsonPath("$.data.orderItems[0].quantity").exists())
                .andExpect(jsonPath("$.data.orderItems[0].lineAmount").exists())
                .andExpect(jsonPath("$.data.buyerEmail").doesNotExist())
                .andExpect(jsonPath("$.data.checkoutOrderId").doesNotExist());
    }

    @Test
    @DisplayName("다른 SELLER의 판매자 주문 상세는 조회할 수 없다")
    void sellerCannotGetAnotherSellersOrderDetail() throws Exception {
        User ownerSeller = userRepository.saveAndFlush(createSeller("owner-seller@example.com", "판매자1"));
        userRepository.saveAndFlush(createSeller("seller@example.com", "판매자2"));
        User buyer = userRepository.saveAndFlush(User.createUser(
                "buyer@example.com",
                passwordEncoder.encode("Password12!"),
                "구매자"
        ));

        Product potato = productRepository.saveAndFlush(createProduct(ownerSeller, "감자", 12000L));
        CheckoutOrder checkoutOrder = CheckoutOrder.create(
                buyer,
                List.of(
                        SellerOrder.create(ownerSeller, List.of(OrderItem.create(potato, 1)))
                )
        );
        checkoutOrder.complete(LocalDateTime.of(2026, 3, 14, 11, 0));
        CheckoutOrder savedOrder = checkoutOrderRepository.saveAndFlush(checkoutOrder);
        Long sellerOrderId = sellerOrderRepository.findAllByCheckoutOrderIdWithItems(savedOrder.getId()).getFirst().getId();

        Cookie sellerSession = login("seller@example.com", "Password12!");

        mockMvc.perform(get("/api/v1/seller-orders/{sellerOrderId}", sellerOrderId)
                        .cookie(sellerSession))
                .andExpect(status().isForbidden())
                .andExpect(jsonPath("$.code").value("AUTH_FORBIDDEN"));
    }

    @Test
    @DisplayName("SELLER 주문 목록 조회는 createdAt 오름차순 정렬을 허용한다")
    void sellerOrdersAllowCreatedAtAscSort() throws Exception {
        User seller = userRepository.saveAndFlush(createSeller("seller@example.com", "판매자"));
        User buyer = userRepository.saveAndFlush(User.createUser(
                "buyer@example.com",
                passwordEncoder.encode("Password12!"),
                "구매자"
        ));

        Product potato = productRepository.saveAndFlush(createProduct(seller, "감자", 12000L));

        CheckoutOrder firstOrder = createCompletedOrder(
                buyer,
                List.of(SellerOrder.create(seller, List.of(OrderItem.create(potato, 1)))),
                LocalDateTime.of(2026, 3, 14, 10, 0)
        );
        CheckoutOrder secondOrder = createCompletedOrder(
                buyer,
                List.of(SellerOrder.create(seller, List.of(OrderItem.create(potato, 2)))),
                LocalDateTime.of(2026, 3, 14, 11, 0)
        );

        checkoutOrderRepository.saveAndFlush(firstOrder);
        checkoutOrderRepository.saveAndFlush(secondOrder);

        Cookie sellerSession = login("seller@example.com", "Password12!");

        mockMvc.perform(get("/api/v1/seller-orders")
                        .cookie(sellerSession)
                        .param("sort", "createdAt,asc"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.data.content[0].orderNumber").value(firstOrder.getOrderNumber()))
                .andExpect(jsonPath("$.data.content[1].orderNumber").value(secondOrder.getOrderNumber()));
    }

    @Test
    @DisplayName("SELLER 주문 목록 조회는 허용되지 않은 정렬 키를 거절한다")
    void sellerOrdersRejectUnsupportedSort() throws Exception {
        userRepository.saveAndFlush(createSeller("seller@example.com", "판매자"));

        Cookie sellerSession = login("seller@example.com", "Password12!");

        mockMvc.perform(get("/api/v1/seller-orders")
                        .cookie(sellerSession)
                        .param("sort", "status,asc"))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.code").value("VALIDATION_ERROR"));
    }

    @Test
    @DisplayName("USER는 판매자 주문 목록을 조회할 수 없다")
    void userCannotGetSellerOrders() throws Exception {
        userRepository.saveAndFlush(User.createUser(
                "buyer@example.com",
                passwordEncoder.encode("Password12!"),
                "구매자"
        ));

        Cookie userSession = login("buyer@example.com", "Password12!");

        mockMvc.perform(get("/api/v1/seller-orders")
                        .cookie(userSession))
                .andExpect(status().isForbidden())
                .andExpect(jsonPath("$.code").value("AUTH_FORBIDDEN"));
    }

    @Test
    @DisplayName("ADMIN은 판매자 주문 목록을 조회할 수 없다")
    void adminCannotGetSellerOrders() throws Exception {
        userRepository.saveAndFlush(User.createAdmin(
                "admin@example.com",
                passwordEncoder.encode("Password12!"),
                "운영자"
        ));

        Cookie adminSession = login("admin@example.com", "Password12!");

        mockMvc.perform(get("/api/v1/seller-orders")
                        .cookie(adminSession))
                .andExpect(status().isForbidden())
                .andExpect(jsonPath("$.code").value("AUTH_FORBIDDEN"));
    }

    @Test
    @DisplayName("미인증 사용자는 판매자 주문 상세를 조회할 수 없다")
    void unauthenticatedUserCannotGetSellerOrderDetail() throws Exception {
        mockMvc.perform(get("/api/v1/seller-orders/{sellerOrderId}", 1L))
                .andExpect(status().isUnauthorized())
                .andExpect(jsonPath("$.code").value("AUTH_UNAUTHORIZED"));
    }

    @Test
    @DisplayName("존재하지 않는 판매자 주문 상세는 404를 반환한다")
    void sellerGetsNotFoundWhenSellerOrderDoesNotExist() throws Exception {
        userRepository.saveAndFlush(createSeller("seller@example.com", "판매자"));

        Cookie sellerSession = login("seller@example.com", "Password12!");

        mockMvc.perform(get("/api/v1/seller-orders/{sellerOrderId}", 999999L)
                        .cookie(sellerSession))
                .andExpect(status().isNotFound())
                .andExpect(jsonPath("$.code").value("ORDER_NOT_FOUND"));
    }

    private CheckoutOrder createCompletedOrder(
            User buyer,
            List<SellerOrder> sellerOrders,
            LocalDateTime completedAt
    ) {
        CheckoutOrder checkoutOrder = CheckoutOrder.create(buyer, sellerOrders);
        checkoutOrder.complete(completedAt);
        return checkoutOrder;
    }

    private Product createProduct(User seller, String name, Long price) {
        return Product.create(
                seller,
                name,
                name + " 상품 설명",
                ProductCategory.AGRICULTURE,
                price,
                10,
                "https://cdn.example.com/" + name + ".jpg"
        );
    }

    private User createSeller(String email, String nickname) {
        User seller = User.createUser(
                email,
                passwordEncoder.encode("Password12!"),
                nickname
        );
        seller.approveSeller(LocalDateTime.of(2026, 3, 13, 10, 0));
        return seller;
    }

    private Cookie login(String email, String password) throws Exception {
        CsrfSession anonymousCsrfSession = fetchCsrfSession();

        MvcResult result = mockMvc.perform(post("/api/v1/auth/login")
                        .contentType(org.springframework.http.MediaType.APPLICATION_JSON)
                        .cookie(anonymousCsrfSession.sessionCookie())
                        .header(anonymousCsrfSession.headerName(), anonymousCsrfSession.token())
                        .content(objectMapper.writeValueAsString(Map.of(
                                "email", email,
                                "password", password
                        ))))
                .andExpect(status().isOk())
                .andReturn();

        Cookie loginSession = result.getResponse().getCookie("SESSION");
        return loginSession != null ? loginSession : anonymousCsrfSession.sessionCookie();
    }

    private CsrfSession fetchCsrfSession() throws Exception {
        MvcResult result = mockMvc.perform(get("/api/v1/csrf"))
                .andExpect(status().isOk())
                .andReturn();

        var data = objectMapper.readTree(result.getResponse().getContentAsString()).get("data");
        Cookie sessionCookie = result.getResponse().getCookie("SESSION");

        return new CsrfSession(
                data.get("headerName").asText(),
                data.get("token").asText(),
                sessionCookie
        );
    }

    private record CsrfSession(
            String headerName,
            String token,
            Cookie sessionCookie
    ) {
    }
}
