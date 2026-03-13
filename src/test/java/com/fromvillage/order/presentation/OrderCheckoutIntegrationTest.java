package com.fromvillage.order.presentation;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.fromvillage.cart.domain.CartItem;
import com.fromvillage.cart.infrastructure.CartJpaRepository;
import com.fromvillage.order.domain.CheckoutOrder;
import com.fromvillage.order.domain.OrderStatus;
import com.fromvillage.order.domain.SellerOrder;
import com.fromvillage.order.infrastructure.CheckoutOrderJpaRepository;
import com.fromvillage.order.infrastructure.SellerOrderJpaRepository;
import com.fromvillage.product.domain.Product;
import com.fromvillage.product.domain.ProductCategory;
import com.fromvillage.product.domain.ProductStatus;
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
import java.util.List;
import java.util.Map;

import static org.assertj.core.api.Assertions.assertThat;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

@SpringBootTest
@AutoConfigureMockMvc
@ActiveProfiles("test")
@Import(TestContainersConfig.class)
class OrderCheckoutIntegrationTest {

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
    private CheckoutOrderJpaRepository checkoutOrderRepository;

    @Autowired
    private SellerOrderJpaRepository sellerOrderRepository;

    @Autowired
    private PasswordEncoder passwordEncoder;

    @BeforeEach
    void setUp() {
        cartRepository.deleteAll();
        sellerOrderRepository.deleteAll();
        checkoutOrderRepository.deleteAll();
        productRepository.deleteAll();
        userRepository.deleteAll();
    }

    @Test
    @DisplayName("USER는 여러 판매자 상품이 담긴 장바구니를 체크아웃할 수 있다")
    void userCanCheckoutCartItemsAcrossMultipleSellers() throws Exception {
        User seller1 = userRepository.saveAndFlush(createSeller("seller1@example.com", "판매자1"));
        User seller2 = userRepository.saveAndFlush(createSeller("seller2@example.com", "판매자2"));
        User buyer = userRepository.saveAndFlush(User.createUser(
                "buyer@example.com",
                passwordEncoder.encode("Password12!"),
                "구매자"
        ));
        User otherBuyer = userRepository.saveAndFlush(User.createUser(
                "other-buyer@example.com",
                passwordEncoder.encode("Password12!"),
                "다른구매자"
        ));

        Product potato = productRepository.saveAndFlush(Product.create(
                seller1,
                "유기농 감자 5kg",
                "해남 햇감자",
                ProductCategory.AGRICULTURE,
                22000L,
                10,
                "https://cdn.example.com/potato.jpg"
        ));
        Product cabbage = productRepository.saveAndFlush(Product.create(
                seller1,
                "절임배추 10kg",
                "전남 해남 절임배추",
                ProductCategory.AGRICULTURE,
                18000L,
                8,
                "https://cdn.example.com/cabbage.jpg"
        ));
        Product mackerel = productRepository.saveAndFlush(Product.create(
                seller2,
                "손질 고등어",
                "당일 손질 고등어",
                ProductCategory.FISHERY,
                15000L,
                5,
                "https://cdn.example.com/mackerel.jpg"
        ));

        cartRepository.saveAndFlush(CartItem.create(buyer, potato, 2));
        cartRepository.saveAndFlush(CartItem.create(buyer, cabbage, 1));
        cartRepository.saveAndFlush(CartItem.create(buyer, mackerel, 3));
        cartRepository.saveAndFlush(CartItem.create(otherBuyer, potato, 1));

        Cookie userSession = login("buyer@example.com", "Password12!");
        CsrfSession csrfSession = fetchCsrfSession(userSession);

        MvcResult result = mockMvc.perform(post("/api/v1/orders/checkout")
                        .cookie(userSession)
                        .contentType(MediaType.APPLICATION_JSON)
                        .header(csrfSession.headerName(), csrfSession.token())
                        .content(objectMapper.writeValueAsString(Map.of())))
                .andExpect(status().isCreated())
                .andExpect(jsonPath("$.success").value(true))
                .andExpect(jsonPath("$.code").value("SUCCESS"))
                .andExpect(jsonPath("$.data.orderId").isNumber())
                .andExpect(jsonPath("$.data.status").value("COMPLETED"))
                .andExpect(jsonPath("$.data.sellerOrderCount").value(2))
                .andExpect(jsonPath("$.data.totalAmount").value(107000))
                .andExpect(jsonPath("$.data.discountAmount").value(0))
                .andExpect(jsonPath("$.data.finalAmount").value(107000))
                .andReturn();

        JsonNode response = objectMapper.readTree(result.getResponse().getContentAsString());
        Long orderId = response.get("data").get("orderId").asLong();

        CheckoutOrder checkoutOrder = checkoutOrderRepository.findByIdWithSellerOrders(orderId).orElseThrow();
        List<SellerOrder> sellerOrders = sellerOrderRepository.findAllByCheckoutOrderIdWithItems(orderId);

        assertThat(checkoutOrderRepository.count()).isEqualTo(1);
        assertThat(checkoutOrder.getUser().getId()).isEqualTo(buyer.getId());
        assertThat(checkoutOrder.getStatus()).isEqualTo(OrderStatus.COMPLETED);
        assertThat(checkoutOrder.getTotalAmount()).isEqualTo(107000L);
        assertThat(checkoutOrder.getDiscountAmount()).isEqualTo(0L);
        assertThat(checkoutOrder.getFinalAmount()).isEqualTo(107000L);

        assertThat(sellerOrders).hasSize(2);
        assertThat(sellerOrders)
                .extracting(order -> order.getSeller().getId())
                .containsExactlyInAnyOrder(seller1.getId(), seller2.getId());

        assertThat(sellerOrders)
                .filteredOn(order -> order.getSeller().getId().equals(seller1.getId()))
                .singleElement()
                .satisfies(order -> {
                    assertThat(order.getStatus()).isEqualTo(OrderStatus.COMPLETED);
                    assertThat(order.getTotalAmount()).isEqualTo(62000L);
                    assertThat(order.getDiscountAmount()).isEqualTo(0L);
                    assertThat(order.getFinalAmount()).isEqualTo(62000L);
                    assertThat(order.getOrderItems()).hasSize(2);
                });

        assertThat(sellerOrders)
                .filteredOn(order -> order.getSeller().getId().equals(seller2.getId()))
                .singleElement()
                .satisfies(order -> {
                    assertThat(order.getStatus()).isEqualTo(OrderStatus.COMPLETED);
                    assertThat(order.getTotalAmount()).isEqualTo(45000L);
                    assertThat(order.getDiscountAmount()).isEqualTo(0L);
                    assertThat(order.getFinalAmount()).isEqualTo(45000L);
                    assertThat(order.getOrderItems()).hasSize(1);
                });

        Product updatedPotato = productRepository.findById(potato.getId()).orElseThrow();
        Product updatedCabbage = productRepository.findById(cabbage.getId()).orElseThrow();
        Product updatedMackerel = productRepository.findById(mackerel.getId()).orElseThrow();

        assertThat(updatedPotato.getStockQuantity()).isEqualTo(8);
        assertThat(updatedPotato.getStatus()).isEqualTo(ProductStatus.ON_SALE);
        assertThat(updatedCabbage.getStockQuantity()).isEqualTo(7);
        assertThat(updatedCabbage.getStatus()).isEqualTo(ProductStatus.ON_SALE);
        assertThat(updatedMackerel.getStockQuantity()).isEqualTo(2);
        assertThat(updatedMackerel.getStatus()).isEqualTo(ProductStatus.ON_SALE);

        assertThat(cartRepository.findAllByUserId(buyer.getId())).isEmpty();
        assertThat(cartRepository.findAllByUserId(otherBuyer.getId())).hasSize(1);

        mockMvc.perform(get("/api/v1/cart-items")
                        .cookie(userSession))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.data.items.length()").value(0))
                .andExpect(jsonPath("$.data.totalItemCount").value(0))
                .andExpect(jsonPath("$.data.totalQuantity").value(0))
                .andExpect(jsonPath("$.data.totalAmount").value(0));
    }

    @Test
    @DisplayName("USER는 단일 상품을 바로 구매할 수 있다")
    void userCanDirectCheckoutSingleProduct() throws Exception {
        User seller = userRepository.saveAndFlush(createSeller("seller@example.com", "판매자"));
        User buyer = userRepository.saveAndFlush(User.createUser(
                "buyer@example.com",
                passwordEncoder.encode("Password12!"),
                "구매자"
        ));

        Product potato = productRepository.saveAndFlush(Product.create(
                seller,
                "유기농 감자 5kg",
                "해남 햇감자",
                ProductCategory.AGRICULTURE,
                22000L,
                10,
                "https://cdn.example.com/potato.jpg"
        ));

        Cookie userSession = login("buyer@example.com", "Password12!");
        CsrfSession csrfSession = fetchCsrfSession(userSession);

        MvcResult result = mockMvc.perform(post("/api/v1/orders/direct-checkout")
                        .cookie(userSession)
                        .contentType(MediaType.APPLICATION_JSON)
                        .header(csrfSession.headerName(), csrfSession.token())
                        .content(objectMapper.writeValueAsString(Map.of(
                                "productId", potato.getId(),
                                "quantity", 2
                        ))))
                .andExpect(status().isCreated())
                .andExpect(jsonPath("$.success").value(true))
                .andExpect(jsonPath("$.code").value("SUCCESS"))
                .andExpect(jsonPath("$.data.orderId").isNumber())
                .andExpect(jsonPath("$.data.status").value("COMPLETED"))
                .andExpect(jsonPath("$.data.sellerOrderCount").value(1))
                .andExpect(jsonPath("$.data.totalAmount").value(44000))
                .andExpect(jsonPath("$.data.discountAmount").value(0))
                .andExpect(jsonPath("$.data.finalAmount").value(44000))
                .andReturn();

        JsonNode response = objectMapper.readTree(result.getResponse().getContentAsString());
        Long orderId = response.get("data").get("orderId").asLong();

        CheckoutOrder checkoutOrder = checkoutOrderRepository.findByIdWithSellerOrders(orderId).orElseThrow();
        List<SellerOrder> sellerOrders = sellerOrderRepository.findAllByCheckoutOrderIdWithItems(orderId);

        assertThat(checkoutOrderRepository.count()).isEqualTo(1);
        assertThat(checkoutOrder.getUser().getId()).isEqualTo(buyer.getId());
        assertThat(checkoutOrder.getStatus()).isEqualTo(OrderStatus.COMPLETED);
        assertThat(checkoutOrder.getTotalAmount()).isEqualTo(44000L);
        assertThat(checkoutOrder.getDiscountAmount()).isEqualTo(0L);
        assertThat(checkoutOrder.getFinalAmount()).isEqualTo(44000L);

        assertThat(sellerOrders).hasSize(1);
        assertThat(sellerOrders.get(0).getSeller().getId()).isEqualTo(seller.getId());
        assertThat(sellerOrders.get(0).getStatus()).isEqualTo(OrderStatus.COMPLETED);
        assertThat(sellerOrders.get(0).getTotalAmount()).isEqualTo(44000L);
        assertThat(sellerOrders.get(0).getDiscountAmount()).isEqualTo(0L);
        assertThat(sellerOrders.get(0).getFinalAmount()).isEqualTo(44000L);
        assertThat(sellerOrders.get(0).getOrderItems()).hasSize(1);

        Product updatedPotato = productRepository.findById(potato.getId()).orElseThrow();
        assertThat(updatedPotato.getStockQuantity()).isEqualTo(8);
        assertThat(updatedPotato.getStatus()).isEqualTo(ProductStatus.ON_SALE);
    }

    @Test
    @DisplayName("soft delete 상품은 바로 구매할 수 없다")
    void directCheckoutRejectsDeletedProduct() throws Exception {
        User seller = userRepository.saveAndFlush(createSeller("seller@example.com", "판매자"));
        User buyer = userRepository.saveAndFlush(User.createUser(
                "buyer@example.com",
                passwordEncoder.encode("Password12!"),
                "구매자"
        ));

        Product potato = productRepository.saveAndFlush(Product.create(
                seller,
                "유기농 감자 5kg",
                "해남 햇감자",
                ProductCategory.AGRICULTURE,
                22000L,
                10,
                "https://cdn.example.com/potato.jpg"
        ));

        potato.softDelete(LocalDateTime.of(2026, 3, 12, 12, 0));
        productRepository.saveAndFlush(potato);

        Cookie userSession = login("buyer@example.com", "Password12!");
        CsrfSession csrfSession = fetchCsrfSession(userSession);

        mockMvc.perform(post("/api/v1/orders/direct-checkout")
                        .cookie(userSession)
                        .contentType(MediaType.APPLICATION_JSON)
                        .header(csrfSession.headerName(), csrfSession.token())
                        .content(objectMapper.writeValueAsString(Map.of(
                                "productId", potato.getId(),
                                "quantity", 2
                        ))))
                .andExpect(status().isConflict())
                .andExpect(jsonPath("$.success").value(false))
                .andExpect(jsonPath("$.code").value("ORDER_PRODUCT_UNAVAILABLE"))
                .andExpect(jsonPath("$.message").value("현재 주문할 수 없는 상품입니다."));

        assertThat(checkoutOrderRepository.findAll()).isEmpty();
        assertThat(sellerOrderRepository.findAll()).isEmpty();

        Product unchangedPotato = productRepository.findById(potato.getId()).orElseThrow();
        assertThat(unchangedPotato.getStockQuantity()).isEqualTo(10);
        assertThat(unchangedPotato.isDeleted()).isTrue();
    }

    @Test
    @DisplayName("바로 구매는 기존 장바구니 항목을 변경하지 않는다")
    void directCheckoutDoesNotChangeCartItems() throws Exception {
        User seller = userRepository.saveAndFlush(createSeller("seller@example.com", "판매자"));
        User buyer = userRepository.saveAndFlush(User.createUser(
                "buyer@example.com",
                passwordEncoder.encode("Password12!"),
                "구매자"
        ));

        Product potato = productRepository.saveAndFlush(Product.create(
                seller,
                "유기농 감자 5kg",
                "해남 햇감자",
                ProductCategory.AGRICULTURE,
                22000L,
                10,
                "https://cdn.example.com/potato.jpg"
        ));
        Product cabbage = productRepository.saveAndFlush(Product.create(
                seller,
                "절임배추 10kg",
                "전남 해남 절임배추",
                ProductCategory.AGRICULTURE,
                18000L,
                8,
                "https://cdn.example.com/cabbage.jpg"
        ));

        cartRepository.saveAndFlush(CartItem.create(buyer, cabbage, 3));

        Cookie userSession = login("buyer@example.com", "Password12!");
        CsrfSession csrfSession = fetchCsrfSession(userSession);

        mockMvc.perform(post("/api/v1/orders/direct-checkout")
                        .cookie(userSession)
                        .contentType(MediaType.APPLICATION_JSON)
                        .header(csrfSession.headerName(), csrfSession.token())
                        .content(objectMapper.writeValueAsString(Map.of(
                                "productId", potato.getId(),
                                "quantity", 2
                        ))))
                .andExpect(status().isCreated())
                .andExpect(jsonPath("$.success").value(true))
                .andExpect(jsonPath("$.data.status").value("COMPLETED"))
                .andExpect(jsonPath("$.data.sellerOrderCount").value(1))
                .andExpect(jsonPath("$.data.totalAmount").value(44000))
                .andExpect(jsonPath("$.data.finalAmount").value(44000));

        List<CartItem> cartItems = cartRepository.findAllByUserId(buyer.getId());

        assertThat(cartItems).hasSize(1);
        assertThat(cartItems.get(0).getProduct().getId()).isEqualTo(cabbage.getId());
        assertThat(cartItems.get(0).getQuantity()).isEqualTo(3);

        mockMvc.perform(get("/api/v1/cart-items")
                        .cookie(userSession))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.data.items.length()").value(1))
                .andExpect(jsonPath("$.data.items[0].productId").value(cabbage.getId()))
                .andExpect(jsonPath("$.data.items[0].quantity").value(3))
                .andExpect(jsonPath("$.data.totalItemCount").value(1))
                .andExpect(jsonPath("$.data.totalQuantity").value(3))
                .andExpect(jsonPath("$.data.totalAmount").value(54000));
    }

    @Test
    @DisplayName("판매 불가 상품은 바로 구매할 수 없다")
    void directCheckoutRejectsSoldOutProduct() throws Exception {
        User seller = userRepository.saveAndFlush(createSeller("seller@example.com", "판매자"));
        User buyer = userRepository.saveAndFlush(User.createUser(
                "buyer@example.com",
                passwordEncoder.encode("Password12!"),
                "구매자"
        ));

        Product cabbage = productRepository.saveAndFlush(Product.create(
                seller,
                "절임배추 10kg",
                "전남 해남 절임배추",
                ProductCategory.AGRICULTURE,
                18000L,
                3,
                "https://cdn.example.com/cabbage.jpg"
        ));

        cabbage.decreaseStock(3);
        productRepository.saveAndFlush(cabbage);

        Cookie userSession = login("buyer@example.com", "Password12!");
        CsrfSession csrfSession = fetchCsrfSession(userSession);

        mockMvc.perform(post("/api/v1/orders/direct-checkout")
                        .cookie(userSession)
                        .contentType(MediaType.APPLICATION_JSON)
                        .header(csrfSession.headerName(), csrfSession.token())
                        .content(objectMapper.writeValueAsString(Map.of(
                                "productId", cabbage.getId(),
                                "quantity", 1
                        ))))
                .andExpect(status().isConflict())
                .andExpect(jsonPath("$.success").value(false))
                .andExpect(jsonPath("$.code").value("ORDER_PRODUCT_UNAVAILABLE"))
                .andExpect(jsonPath("$.message").value("현재 주문할 수 없는 상품입니다."));

        assertThat(checkoutOrderRepository.findAll()).isEmpty();
        assertThat(sellerOrderRepository.findAll()).isEmpty();

        Product soldOutCabbage = productRepository.findById(cabbage.getId()).orElseThrow();
        assertThat(soldOutCabbage.getStockQuantity()).isEqualTo(0);
        assertThat(soldOutCabbage.getStatus()).isEqualTo(ProductStatus.SOLD_OUT);
    }

    @Test
    @DisplayName("바로 구매 시점에 재고가 부족하면 주문은 실패한다")
    void directCheckoutRejectsInsufficientStock() throws Exception {
        User seller = userRepository.saveAndFlush(createSeller("seller@example.com", "판매자"));
        User buyer = userRepository.saveAndFlush(User.createUser(
                "buyer@example.com",
                passwordEncoder.encode("Password12!"),
                "구매자"
        ));

        Product potato = productRepository.saveAndFlush(Product.create(
                seller,
                "유기농 감자 5kg",
                "해남 햇감자",
                ProductCategory.AGRICULTURE,
                22000L,
                2,
                "https://cdn.example.com/potato.jpg"
        ));

        Cookie userSession = login("buyer@example.com", "Password12!");
        CsrfSession csrfSession = fetchCsrfSession(userSession);

        mockMvc.perform(post("/api/v1/orders/direct-checkout")
                        .cookie(userSession)
                        .contentType(MediaType.APPLICATION_JSON)
                        .header(csrfSession.headerName(), csrfSession.token())
                        .content(objectMapper.writeValueAsString(Map.of(
                                "productId", potato.getId(),
                                "quantity", 3
                        ))))
                .andExpect(status().isConflict())
                .andExpect(jsonPath("$.success").value(false))
                .andExpect(jsonPath("$.code").value("PRODUCT_STOCK_INSUFFICIENT"))
                .andExpect(jsonPath("$.message").value("상품 재고가 부족합니다."));

        assertThat(checkoutOrderRepository.findAll()).isEmpty();
        assertThat(sellerOrderRepository.findAll()).isEmpty();

        Product unchangedPotato = productRepository.findById(potato.getId()).orElseThrow();
        assertThat(unchangedPotato.getStockQuantity()).isEqualTo(2);
        assertThat(unchangedPotato.getStatus()).isEqualTo(ProductStatus.ON_SALE);
    }

    @Test
    @DisplayName("SELLER는 바로 구매 API에 접근할 수 없다")
    void sellerCannotDirectCheckout() throws Exception {
        User seller = userRepository.saveAndFlush(createSeller("seller@example.com", "판매자"));
        Product potato = productRepository.saveAndFlush(Product.create(
                seller,
                "유기농 감자 5kg",
                "해남 햇감자",
                ProductCategory.AGRICULTURE,
                22000L,
                10,
                "https://cdn.example.com/potato.jpg"
        ));

        Cookie sellerSession = login("seller@example.com", "Password12!");
        CsrfSession csrfSession = fetchCsrfSession(sellerSession);

        mockMvc.perform(post("/api/v1/orders/direct-checkout")
                        .cookie(sellerSession)
                        .contentType(MediaType.APPLICATION_JSON)
                        .header(csrfSession.headerName(), csrfSession.token())
                        .content(objectMapper.writeValueAsString(Map.of(
                                "productId", potato.getId(),
                                "quantity", 1
                        ))))
                .andExpect(status().isForbidden())
                .andExpect(jsonPath("$.code").value("AUTH_FORBIDDEN"));
    }

    @Test
    @DisplayName("ADMIN은 바로 구매 API에 접근할 수 없다")
    void adminCannotDirectCheckout() throws Exception {
        userRepository.saveAndFlush(User.createAdmin(
                "admin@example.com",
                passwordEncoder.encode("Password12!"),
                "운영자"
        ));
        User seller = userRepository.saveAndFlush(createSeller("seller@example.com", "판매자"));
        Product potato = productRepository.saveAndFlush(Product.create(
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

        mockMvc.perform(post("/api/v1/orders/direct-checkout")
                        .cookie(adminSession)
                        .contentType(MediaType.APPLICATION_JSON)
                        .header(csrfSession.headerName(), csrfSession.token())
                        .content(objectMapper.writeValueAsString(Map.of(
                                "productId", potato.getId(),
                                "quantity", 1
                        ))))
                .andExpect(status().isForbidden())
                .andExpect(jsonPath("$.code").value("AUTH_FORBIDDEN"));
    }

    @Test
    @DisplayName("미인증 사용자는 바로 구매 API에 접근할 수 없다")
    void unauthenticatedUserCannotDirectCheckout() throws Exception {
        User seller = userRepository.saveAndFlush(createSeller("seller@example.com", "판매자"));
        Product potato = productRepository.saveAndFlush(Product.create(
                seller,
                "유기농 감자 5kg",
                "해남 햇감자",
                ProductCategory.AGRICULTURE,
                22000L,
                10,
                "https://cdn.example.com/potato.jpg"
        ));

        CsrfSession anonymousCsrfSession = fetchCsrfSession();

        mockMvc.perform(post("/api/v1/orders/direct-checkout")
                        .cookie(anonymousCsrfSession.sessionCookie())
                        .contentType(MediaType.APPLICATION_JSON)
                        .header(anonymousCsrfSession.headerName(), anonymousCsrfSession.token())
                        .content(objectMapper.writeValueAsString(Map.of(
                                "productId", potato.getId(),
                                "quantity", 1
                        ))))
                .andExpect(status().isUnauthorized())
                .andExpect(jsonPath("$.code").value("AUTH_UNAUTHORIZED"));
    }

    @Test
    @DisplayName("바로 구매는 CSRF 토큰이 없으면 거절된다")
    void directCheckoutRequiresCsrf() throws Exception {
        User seller = userRepository.saveAndFlush(createSeller("seller@example.com", "판매자"));
        User buyer = userRepository.saveAndFlush(User.createUser(
                "buyer@example.com",
                passwordEncoder.encode("Password12!"),
                "구매자"
        ));
        Product potato = productRepository.saveAndFlush(Product.create(
                seller,
                "유기농 감자 5kg",
                "해남 햇감자",
                ProductCategory.AGRICULTURE,
                22000L,
                10,
                "https://cdn.example.com/potato.jpg"
        ));

        Cookie userSession = login("buyer@example.com", "Password12!");

        mockMvc.perform(post("/api/v1/orders/direct-checkout")
                        .cookie(userSession)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(Map.of(
                                "productId", potato.getId(),
                                "quantity", 1
                        ))))
                .andExpect(status().isForbidden())
                .andExpect(jsonPath("$.code").value("AUTH_CSRF_INVALID"));
    }
    @Test
    @DisplayName("체크아웃으로 재고가 정확히 0이 되면 상품은 SOLD_OUT으로 전이된다")
    void checkoutMarksProductSoldOutWhenStockBecomesZero() throws Exception {
        User seller = userRepository.saveAndFlush(createSeller("seller@example.com", "판매자"));
        User buyer = userRepository.saveAndFlush(User.createUser(
                "buyer@example.com",
                passwordEncoder.encode("Password12!"),
                "구매자"
        ));

        Product mackerel = productRepository.saveAndFlush(Product.create(
                seller,
                "손질 고등어",
                "당일 손질 고등어",
                ProductCategory.FISHERY,
                15000L,
                3,
                "https://cdn.example.com/mackerel.jpg"
        ));

        cartRepository.saveAndFlush(CartItem.create(buyer, mackerel, 3));

        Cookie userSession = login("buyer@example.com", "Password12!");
        CsrfSession csrfSession = fetchCsrfSession(userSession);

        mockMvc.perform(post("/api/v1/orders/checkout")
                        .cookie(userSession)
                        .contentType(MediaType.APPLICATION_JSON)
                        .header(csrfSession.headerName(), csrfSession.token())
                        .content(objectMapper.writeValueAsString(Map.of())))
                .andExpect(status().isCreated())
                .andExpect(jsonPath("$.success").value(true))
                .andExpect(jsonPath("$.data.status").value("COMPLETED"))
                .andExpect(jsonPath("$.data.sellerOrderCount").value(1))
                .andExpect(jsonPath("$.data.totalAmount").value(45000))
                .andExpect(jsonPath("$.data.finalAmount").value(45000));

        Product soldOutMackerel = productRepository.findById(mackerel.getId()).orElseThrow();

        assertThat(soldOutMackerel.getStockQuantity()).isEqualTo(0);
        assertThat(soldOutMackerel.getStatus()).isEqualTo(ProductStatus.SOLD_OUT);
        assertThat(cartRepository.findAllByUserId(buyer.getId())).isEmpty();
    }

    @Test
    @DisplayName("빈 장바구니는 체크아웃할 수 없다")
    void checkoutRejectsEmptyCart() throws Exception {
        userRepository.saveAndFlush(User.createUser(
                "buyer@example.com",
                passwordEncoder.encode("Password12!"),
                "구매자"
        ));

        Cookie userSession = login("buyer@example.com", "Password12!");
        CsrfSession csrfSession = fetchCsrfSession(userSession);

        mockMvc.perform(post("/api/v1/orders/checkout")
                        .cookie(userSession)
                        .contentType(MediaType.APPLICATION_JSON)
                        .header(csrfSession.headerName(), csrfSession.token())
                        .content(objectMapper.writeValueAsString(Map.of())))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.success").value(false))
                .andExpect(jsonPath("$.code").value("ORDER_CHECKOUT_CART_EMPTY"))
                .andExpect(jsonPath("$.message").value("체크아웃할 장바구니 항목이 없습니다."));

        assertThat(checkoutOrderRepository.findAll()).isEmpty();
        assertThat(sellerOrderRepository.findAll()).isEmpty();
        assertThat(cartRepository.findAll()).isEmpty();
    }

    @Test
    @DisplayName("soft delete 상품이 포함되면 체크아웃은 전체 실패한다")
    void checkoutRejectsDeletedProductInCart() throws Exception {
        User seller = userRepository.saveAndFlush(createSeller("seller@example.com", "판매자"));
        User buyer = userRepository.saveAndFlush(User.createUser(
                "buyer@example.com",
                passwordEncoder.encode("Password12!"),
                "구매자"
        ));

        Product potato = productRepository.saveAndFlush(Product.create(
                seller,
                "유기농 감자 5kg",
                "해남 햇감자",
                ProductCategory.AGRICULTURE,
                22000L,
                10,
                "https://cdn.example.com/potato.jpg"
        ));
        Product mackerel = productRepository.saveAndFlush(Product.create(
                seller,
                "손질 고등어",
                "당일 손질 고등어",
                ProductCategory.FISHERY,
                15000L,
                5,
                "https://cdn.example.com/mackerel.jpg"
        ));

        cartRepository.saveAndFlush(CartItem.create(buyer, potato, 2));
        cartRepository.saveAndFlush(CartItem.create(buyer, mackerel, 1));

        mackerel.softDelete(LocalDateTime.of(2026, 3, 12, 12, 0));
        productRepository.saveAndFlush(mackerel);

        Cookie userSession = login("buyer@example.com", "Password12!");
        CsrfSession csrfSession = fetchCsrfSession(userSession);

        mockMvc.perform(post("/api/v1/orders/checkout")
                        .cookie(userSession)
                        .contentType(MediaType.APPLICATION_JSON)
                        .header(csrfSession.headerName(), csrfSession.token())
                        .content(objectMapper.writeValueAsString(Map.of())))
                .andExpect(status().isConflict())
                .andExpect(jsonPath("$.success").value(false))
                .andExpect(jsonPath("$.code").value("ORDER_PRODUCT_UNAVAILABLE"))
                .andExpect(jsonPath("$.message").value("현재 주문할 수 없는 상품입니다."));

        assertThat(checkoutOrderRepository.findAll()).isEmpty();
        assertThat(sellerOrderRepository.findAll()).isEmpty();
        assertThat(cartRepository.findAllByUserId(buyer.getId())).hasSize(2);

        Product unchangedPotato = productRepository.findById(potato.getId()).orElseThrow();
        Product unchangedMackerel = productRepository.findById(mackerel.getId()).orElseThrow();

        assertThat(unchangedPotato.getStockQuantity()).isEqualTo(10);
        assertThat(unchangedPotato.getStatus()).isEqualTo(ProductStatus.ON_SALE);
        assertThat(unchangedMackerel.getStockQuantity()).isEqualTo(5);
        assertThat(unchangedMackerel.isDeleted()).isTrue();
    }

    @Test
    @DisplayName("판매 불가 상품이 포함되면 체크아웃은 전체 실패한다")
    void checkoutRejectsSoldOutProductInCart() throws Exception {
        User seller = userRepository.saveAndFlush(createSeller("seller@example.com", "판매자"));
        User buyer = userRepository.saveAndFlush(User.createUser(
                "buyer@example.com",
                passwordEncoder.encode("Password12!"),
                "구매자"
        ));

        Product potato = productRepository.saveAndFlush(Product.create(
                seller,
                "유기농 감자 5kg",
                "해남 햇감자",
                ProductCategory.AGRICULTURE,
                22000L,
                10,
                "https://cdn.example.com/potato.jpg"
        ));
        Product cabbage = productRepository.saveAndFlush(Product.create(
                seller,
                "절임배추 10kg",
                "전남 해남 절임배추",
                ProductCategory.AGRICULTURE,
                18000L,
                3,
                "https://cdn.example.com/cabbage.jpg"
        ));

        cartRepository.saveAndFlush(CartItem.create(buyer, potato, 2));
        cartRepository.saveAndFlush(CartItem.create(buyer, cabbage, 1));

        cabbage.decreaseStock(3);
        productRepository.saveAndFlush(cabbage);

        Cookie userSession = login("buyer@example.com", "Password12!");
        CsrfSession csrfSession = fetchCsrfSession(userSession);

        mockMvc.perform(post("/api/v1/orders/checkout")
                        .cookie(userSession)
                        .contentType(MediaType.APPLICATION_JSON)
                        .header(csrfSession.headerName(), csrfSession.token())
                        .content(objectMapper.writeValueAsString(Map.of())))
                .andExpect(status().isConflict())
                .andExpect(jsonPath("$.success").value(false))
                .andExpect(jsonPath("$.code").value("ORDER_PRODUCT_UNAVAILABLE"))
                .andExpect(jsonPath("$.message").value("현재 주문할 수 없는 상품입니다."));

        assertThat(checkoutOrderRepository.findAll()).isEmpty();
        assertThat(sellerOrderRepository.findAll()).isEmpty();
        assertThat(cartRepository.findAllByUserId(buyer.getId())).hasSize(2);

        Product unchangedPotato = productRepository.findById(potato.getId()).orElseThrow();
        Product soldOutCabbage = productRepository.findById(cabbage.getId()).orElseThrow();

        assertThat(unchangedPotato.getStockQuantity()).isEqualTo(10);
        assertThat(unchangedPotato.getStatus()).isEqualTo(ProductStatus.ON_SALE);
        assertThat(soldOutCabbage.getStockQuantity()).isEqualTo(0);
        assertThat(soldOutCabbage.getStatus()).isEqualTo(ProductStatus.SOLD_OUT);
    }

    @Test
    @DisplayName("체크아웃 시점에 재고가 부족하면 체크아웃은 전체 실패한다")
    void checkoutRejectsWhenStockIsInsufficientAtCheckoutTime() throws Exception {
        User seller = userRepository.saveAndFlush(createSeller("seller@example.com", "판매자"));
        User buyer = userRepository.saveAndFlush(User.createUser(
                "buyer@example.com",
                passwordEncoder.encode("Password12!"),
                "구매자"
        ));

        Product potato = productRepository.saveAndFlush(Product.create(
                seller,
                "유기농 감자 5kg",
                "해남 햇감자",
                ProductCategory.AGRICULTURE,
                22000L,
                10,
                "https://cdn.example.com/potato.jpg"
        ));
        Product cabbage = productRepository.saveAndFlush(Product.create(
                seller,
                "절임배추 10kg",
                "전남 해남 절임배추",
                ProductCategory.AGRICULTURE,
                18000L,
                5,
                "https://cdn.example.com/cabbage.jpg"
        ));

        cartRepository.saveAndFlush(CartItem.create(buyer, potato, 1));
        cartRepository.saveAndFlush(CartItem.create(buyer, cabbage, 4));

        cabbage.decreaseStock(3);
        productRepository.saveAndFlush(cabbage);

        Cookie userSession = login("buyer@example.com", "Password12!");
        CsrfSession csrfSession = fetchCsrfSession(userSession);

        mockMvc.perform(post("/api/v1/orders/checkout")
                        .cookie(userSession)
                        .contentType(MediaType.APPLICATION_JSON)
                        .header(csrfSession.headerName(), csrfSession.token())
                        .content(objectMapper.writeValueAsString(Map.of())))
                .andExpect(status().isConflict())
                .andExpect(jsonPath("$.success").value(false))
                .andExpect(jsonPath("$.code").value("PRODUCT_STOCK_INSUFFICIENT"))
                .andExpect(jsonPath("$.message").value("상품 재고가 부족합니다."));

        assertThat(checkoutOrderRepository.findAll()).isEmpty();
        assertThat(sellerOrderRepository.findAll()).isEmpty();
        assertThat(cartRepository.findAllByUserId(buyer.getId())).hasSize(2);

        Product unchangedPotato = productRepository.findById(potato.getId()).orElseThrow();
        Product insufficientCabbage = productRepository.findById(cabbage.getId()).orElseThrow();

        assertThat(unchangedPotato.getStockQuantity()).isEqualTo(10);
        assertThat(unchangedPotato.getStatus()).isEqualTo(ProductStatus.ON_SALE);
        assertThat(insufficientCabbage.getStockQuantity()).isEqualTo(2);
        assertThat(insufficientCabbage.getStatus()).isEqualTo(ProductStatus.ON_SALE);
    }

    @Test
    @DisplayName("SELLER는 체크아웃 API에 접근할 수 없다")
    void sellerCannotCheckout() throws Exception {
        userRepository.saveAndFlush(createSeller("seller@example.com", "판매자"));

        Cookie sellerSession = login("seller@example.com", "Password12!");
        CsrfSession csrfSession = fetchCsrfSession(sellerSession);

        mockMvc.perform(post("/api/v1/orders/checkout")
                        .cookie(sellerSession)
                        .contentType(MediaType.APPLICATION_JSON)
                        .header(csrfSession.headerName(), csrfSession.token())
                        .content(objectMapper.writeValueAsString(Map.of())))
                .andExpect(status().isForbidden())
                .andExpect(jsonPath("$.code").value("AUTH_FORBIDDEN"));
    }

    @Test
    @DisplayName("ADMIN은 체크아웃 API에 접근할 수 없다")
    void adminCannotCheckout() throws Exception {
        userRepository.saveAndFlush(User.createAdmin(
                "admin@example.com",
                passwordEncoder.encode("Password12!"),
                "운영자"
        ));

        Cookie adminSession = login("admin@example.com", "Password12!");
        CsrfSession csrfSession = fetchCsrfSession(adminSession);

        mockMvc.perform(post("/api/v1/orders/checkout")
                        .cookie(adminSession)
                        .contentType(MediaType.APPLICATION_JSON)
                        .header(csrfSession.headerName(), csrfSession.token())
                        .content(objectMapper.writeValueAsString(Map.of())))
                .andExpect(status().isForbidden())
                .andExpect(jsonPath("$.code").value("AUTH_FORBIDDEN"));
    }

    @Test
    @DisplayName("미인증 사용자는 체크아웃 API에 접근할 수 없다")
    void unauthenticatedUserCannotCheckout() throws Exception {
        CsrfSession anonymousCsrfSession = fetchCsrfSession();

        mockMvc.perform(post("/api/v1/orders/checkout")
                        .cookie(anonymousCsrfSession.sessionCookie())
                        .contentType(MediaType.APPLICATION_JSON)
                        .header(anonymousCsrfSession.headerName(), anonymousCsrfSession.token())
                        .content(objectMapper.writeValueAsString(Map.of())))
                .andExpect(status().isUnauthorized())
                .andExpect(jsonPath("$.code").value("AUTH_UNAUTHORIZED"));
    }

    @Test
    @DisplayName("체크아웃은 CSRF 토큰이 없으면 거절된다")
    void checkoutRequiresCsrf() throws Exception {
        userRepository.saveAndFlush(User.createUser(
                "buyer@example.com",
                passwordEncoder.encode("Password12!"),
                "구매자"
        ));

        Cookie userSession = login("buyer@example.com", "Password12!");

        mockMvc.perform(post("/api/v1/orders/checkout")
                        .cookie(userSession)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(Map.of())))
                .andExpect(status().isForbidden())
                .andExpect(jsonPath("$.code").value("AUTH_CSRF_INVALID"));
    }

    private User createSeller(String email, String nickname) {
        User seller = User.createUser(
                email,
                passwordEncoder.encode("Password12!"),
                nickname
        );
        seller.approveSeller(LocalDateTime.of(2026, 3, 12, 10, 0));
        return seller;
    }

    private Cookie login(String email, String password) throws Exception {
        CsrfSession anonymousCsrfSession = fetchCsrfSession();

        MvcResult result = mockMvc.perform(post("/api/v1/auth/login")
                        .contentType(MediaType.APPLICATION_JSON)
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

        JsonNode data = objectMapper.readTree(result.getResponse().getContentAsString()).get("data");
        Cookie sessionCookie = result.getResponse().getCookie("SESSION");

        return new CsrfSession(
                data.get("headerName").asText(),
                data.get("token").asText(),
                sessionCookie
        );
    }

    private CsrfSession fetchCsrfSession(Cookie sessionCookie) throws Exception {
        MvcResult result = mockMvc.perform(get("/api/v1/csrf")
                        .cookie(sessionCookie))
                .andExpect(status().isOk())
                .andReturn();

        JsonNode data = objectMapper.readTree(result.getResponse().getContentAsString()).get("data");
        Cookie refreshedSessionCookie = result.getResponse().getCookie("SESSION");
        Cookie effectiveSessionCookie = refreshedSessionCookie != null ? refreshedSessionCookie : sessionCookie;

        return new CsrfSession(
                data.get("headerName").asText(),
                data.get("token").asText(),
                effectiveSessionCookie
        );
    }

    private record CsrfSession(
            String headerName,
            String token,
            Cookie sessionCookie
    ) {
    }
}
