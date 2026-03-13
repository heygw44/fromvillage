package com.fromvillage.order.presentation;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.fromvillage.order.domain.CheckoutOrder;
import com.fromvillage.order.domain.OrderItem;
import com.fromvillage.order.domain.SellerOrder;
import com.fromvillage.order.infrastructure.CheckoutOrderJpaRepository;
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
import org.springframework.test.util.ReflectionTestUtils;
import com.fromvillage.order.domain.OrderStatus;
import com.fromvillage.order.infrastructure.SellerOrderJpaRepository;
import org.springframework.http.MediaType;

import java.time.LocalDateTime;
import java.util.List;
import java.util.Map;

import static org.hamcrest.Matchers.hasItems;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.assertj.core.api.Assertions.assertThat;

@SpringBootTest
@AutoConfigureMockMvc
@ActiveProfiles("test")
@Import(TestContainersConfig.class)
class OrderQueryIntegrationTest {

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
    private PasswordEncoder passwordEncoder;

    @Autowired
    private SellerOrderJpaRepository sellerOrderRepository;

    @BeforeEach
    void setUp() {
        sellerOrderRepository.deleteAll();
        checkoutOrderRepository.deleteAll();
        productRepository.deleteAll();
        userRepository.deleteAll();
    }

    @Test
    @DisplayName("USER는 본인 주문 목록을 페이지로 조회할 수 있다")
    void userCanGetOwnOrders() throws Exception {
        User seller1 = userRepository.saveAndFlush(createSeller("seller1@example.com", "판매자1"));
        User seller2 = userRepository.saveAndFlush(createSeller("seller2@example.com", "판매자2"));
        User buyer = userRepository.saveAndFlush(User.createUser(
                "buyer@example.com",
                passwordEncoder.encode("Password12!"),
                "구매자"
        ));
        User otherBuyer = userRepository.saveAndFlush(User.createUser(
                "otherbuyer@example.com",
                passwordEncoder.encode("Password12!"),
                "다른구매자"
        ));

        Product potato = productRepository.saveAndFlush(createProduct(seller1, "감자", 12000L));
        Product cabbage = productRepository.saveAndFlush(createProduct(seller2, "배추", 8000L));
        Product mackerel = productRepository.saveAndFlush(createProduct(seller2, "고등어", 15000L));

        CheckoutOrder firstOrder = createCompletedOrder(
                buyer,
                List.of(
                        SellerOrder.create(seller1, List.of(OrderItem.create(potato, 2))),
                        SellerOrder.create(seller2, List.of(OrderItem.create(cabbage, 1)))
                ),
                LocalDateTime.of(2026, 3, 13, 10, 0)
        );
        CheckoutOrder secondOrder = createCompletedOrder(
                buyer,
                List.of(
                        SellerOrder.create(seller2, List.of(OrderItem.create(mackerel, 3)))
                ),
                LocalDateTime.of(2026, 3, 13, 11, 0)
        );
        CheckoutOrder otherBuyerOrder = createCompletedOrder(
                otherBuyer,
                List.of(
                        SellerOrder.create(seller1, List.of(OrderItem.create(potato, 1)))
                ),
                LocalDateTime.of(2026, 3, 13, 12, 0)
        );

        checkoutOrderRepository.saveAndFlush(firstOrder);
        checkoutOrderRepository.saveAndFlush(secondOrder);
        checkoutOrderRepository.saveAndFlush(otherBuyerOrder);

        Cookie userSession = login("buyer@example.com", "Password12!");

        mockMvc.perform(get("/api/v1/orders")
                        .cookie(userSession)
                        .param("page", "0")
                        .param("size", "20"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.success").value(true))
                .andExpect(jsonPath("$.code").value("SUCCESS"))
                .andExpect(jsonPath("$.data.content.length()").value(2))
                .andExpect(jsonPath("$.data.totalElements").value(2))
                .andExpect(jsonPath("$.data.totalPages").value(1))
                .andExpect(jsonPath("$.data.hasNext").value(false))
                .andExpect(jsonPath("$.data.content[?(@.orderNumber == '%s')]",
                        orderNumber(firstOrder)).exists())
                .andExpect(jsonPath("$.data.content[?(@.orderNumber == '%s')]",
                        orderNumber(secondOrder)).exists())
                .andExpect(jsonPath("$.data.content[?(@.orderNumber == '%s')]",
                        orderNumber(otherBuyerOrder)).isEmpty())
                .andExpect(jsonPath("$.data.content[0].orderId").doesNotExist());
    }

    @Test
    @DisplayName("내 주문 목록은 최신 생성 순으로 정렬된다")
    void ownOrdersAreSortedByCreatedAtDesc() throws Exception {
        User seller = userRepository.saveAndFlush(createSeller("seller@example.com", "판매자"));
        User buyer = userRepository.saveAndFlush(User.createUser(
                "buyer@example.com",
                passwordEncoder.encode("Password12!"),
                "구매자"
        ));

        Product potato = productRepository.saveAndFlush(createProduct(seller, "감자", 12000L));

        CheckoutOrder firstOrder = checkoutOrderRepository.saveAndFlush(createCompletedOrder(
                buyer,
                List.of(SellerOrder.create(seller, List.of(OrderItem.create(potato, 1)))),
                LocalDateTime.of(2026, 3, 13, 10, 0)
        ));
        CheckoutOrder secondOrder = checkoutOrderRepository.saveAndFlush(createCompletedOrder(
                buyer,
                List.of(SellerOrder.create(seller, List.of(OrderItem.create(potato, 2)))),
                LocalDateTime.of(2026, 3, 13, 11, 0)
        ));

        Cookie userSession = login("buyer@example.com", "Password12!");

        mockMvc.perform(get("/api/v1/orders")
                        .cookie(userSession))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.data.content[0].orderNumber").value(orderNumber(secondOrder)))
                .andExpect(jsonPath("$.data.content[1].orderNumber").value(orderNumber(firstOrder)))
                .andExpect(jsonPath("$.data.content[0].orderId").doesNotExist());
    }

    @Test
    @DisplayName("내 주문 목록 조회는 createdAt 오름차순 정렬을 허용한다")
    void ownOrdersAllowCreatedAtAscSort() throws Exception {
        User seller = userRepository.saveAndFlush(createSeller("seller@example.com", "판매자"));
        User buyer = userRepository.saveAndFlush(User.createUser(
                "buyer@example.com",
                passwordEncoder.encode("Password12!"),
                "구매자"
        ));

        Product potato = productRepository.saveAndFlush(createProduct(seller, "감자", 12000L));

        CheckoutOrder firstOrder = checkoutOrderRepository.saveAndFlush(createCompletedOrder(
                buyer,
                List.of(SellerOrder.create(seller, List.of(OrderItem.create(potato, 1)))),
                LocalDateTime.of(2026, 3, 13, 10, 0)
        ));
        CheckoutOrder secondOrder = checkoutOrderRepository.saveAndFlush(createCompletedOrder(
                buyer,
                List.of(SellerOrder.create(seller, List.of(OrderItem.create(potato, 2)))),
                LocalDateTime.of(2026, 3, 13, 11, 0)
        ));

        Cookie userSession = login("buyer@example.com", "Password12!");

        mockMvc.perform(get("/api/v1/orders")
                        .cookie(userSession)
                        .param("sort", "createdAt,asc"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.data.content[0].orderNumber").value(orderNumber(firstOrder)))
                .andExpect(jsonPath("$.data.content[1].orderNumber").value(orderNumber(secondOrder)))
                .andExpect(jsonPath("$.data.content[0].orderId").doesNotExist());
    }

    @Test
    @DisplayName("내 주문 목록 조회는 createdAt 외의 정렬 키를 허용하지 않는다")
    void ownOrdersRejectUnsupportedSort() throws Exception {
        userRepository.saveAndFlush(User.createUser(
                "buyer@example.com",
                passwordEncoder.encode("Password12!"),
                "구매자"
        ));

        Cookie userSession = login("buyer@example.com", "Password12!");

        mockMvc.perform(get("/api/v1/orders")
                        .cookie(userSession)
                        .param("sort", "id,asc"))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.code").value("VALIDATION_ERROR"));
    }

    @Test
    @DisplayName("내 주문 목록 조회는 복수 정렬 키를 허용하지 않는다")
    void ownOrdersRejectMultipleSorts() throws Exception {
        userRepository.saveAndFlush(User.createUser(
                "buyer@example.com",
                passwordEncoder.encode("Password12!"),
                "구매자"
        ));

        Cookie userSession = login("buyer@example.com", "Password12!");

        mockMvc.perform(get("/api/v1/orders")
                        .cookie(userSession)
                        .param("sort", "createdAt,desc", "id,asc"))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.code").value("VALIDATION_ERROR"));
    }

    @Test
    @DisplayName("SELLER는 내 주문 목록을 조회할 수 없다")
    void sellerCannotGetOwnOrders() throws Exception {
        userRepository.saveAndFlush(createSeller("seller@example.com", "판매자"));

        Cookie sellerSession = login("seller@example.com", "Password12!");

        mockMvc.perform(get("/api/v1/orders")
                        .cookie(sellerSession))
                .andExpect(status().isForbidden())
                .andExpect(jsonPath("$.code").value("AUTH_FORBIDDEN"));
    }

    @Test
    @DisplayName("ADMIN은 내 주문 목록을 조회할 수 없다")
    void adminCannotGetOwnOrders() throws Exception {
        userRepository.saveAndFlush(User.createAdmin(
                "admin@example.com",
                passwordEncoder.encode("Password12!"),
                "운영자"
        ));

        Cookie adminSession = login("admin@example.com", "Password12!");

        mockMvc.perform(get("/api/v1/orders")
                        .cookie(adminSession))
                .andExpect(status().isForbidden())
                .andExpect(jsonPath("$.code").value("AUTH_FORBIDDEN"));
    }

    @Test
    @DisplayName("미인증 사용자는 내 주문 목록을 조회할 수 없다")
    void unauthenticatedUserCannotGetOwnOrders() throws Exception {
        mockMvc.perform(get("/api/v1/orders"))
                .andExpect(status().isUnauthorized())
                .andExpect(jsonPath("$.code").value("AUTH_UNAUTHORIZED"));
    }

    @Test
    @DisplayName("USER는 본인 주문 상세를 sellerOrders와 orderItems까지 조회할 수 있다")
    void userCanGetOwnOrderDetail() throws Exception {
        User seller1 = userRepository.saveAndFlush(createSeller("seller1@example.com", "판매자1"));
        User seller2 = userRepository.saveAndFlush(createSeller("seller2@example.com", "판매자2"));
        User buyer = userRepository.saveAndFlush(User.createUser(
                "buyer@example.com",
                passwordEncoder.encode("Password12!"),
                "구매자"
        ));

        Product potato = productRepository.saveAndFlush(createProduct(seller1, "감자", 12000L));
        Product cabbage = productRepository.saveAndFlush(createProduct(seller2, "배추", 8000L));

        CheckoutOrder order = createCompletedOrder(
                buyer,
                List.of(
                        SellerOrder.create(seller1, List.of(OrderItem.create(potato, 2))),
                        SellerOrder.create(seller2, List.of(OrderItem.create(cabbage, 1)))
                ),
                LocalDateTime.of(2026, 3, 13, 11, 0)
        );
        CheckoutOrder savedOrder = checkoutOrderRepository.saveAndFlush(order);

        Cookie userSession = login("buyer@example.com", "Password12!");

        mockMvc.perform(get("/api/v1/orders/{orderNumber}", savedOrder.getOrderNumber())
                        .cookie(userSession))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.success").value(true))
                .andExpect(jsonPath("$.code").value("SUCCESS"))
                .andExpect(jsonPath("$.data.orderNumber").value(orderNumber(savedOrder)))
                .andExpect(jsonPath("$.data.orderId").doesNotExist())
                .andExpect(jsonPath("$.data.status").value("COMPLETED"))
                .andExpect(jsonPath("$.data.sellerOrders.length()").value(2))
                .andExpect(jsonPath("$.data.sellerOrders[0].sellerOrderId").doesNotExist())
                .andExpect(jsonPath("$.data.sellerOrders[0].sellerId").doesNotExist())
                .andExpect(jsonPath("$.data.sellerOrders[0].orderItems[0].orderItemId").doesNotExist())
                .andExpect(jsonPath("$.data.sellerOrders[0].orderItems[0].productId").doesNotExist())
                .andExpect(jsonPath("$.data.sellerOrders[*].orderItems[*].productNameSnapshot", hasItems("감자", "배추")))
                .andExpect(jsonPath("$.data.sellerOrders[*].orderItems[*].quantity", hasItems(2, 1)));
    }

    @Test
    @DisplayName("USER는 타인 주문 상세를 조회할 수 없다")
    void userCannotGetAnotherUsersOrderDetail() throws Exception {
        User seller = userRepository.saveAndFlush(createSeller("seller@example.com", "판매자"));
        User buyer = userRepository.saveAndFlush(User.createUser(
                "buyer@example.com",
                passwordEncoder.encode("Password12!"),
                "구매자"
        ));
        User otherBuyer = userRepository.saveAndFlush(User.createUser(
                "otherbuyer@example.com",
                passwordEncoder.encode("Password12!"),
                "다른구매자"
        ));

        Product potato = productRepository.saveAndFlush(createProduct(seller, "감자", 12000L));

        CheckoutOrder order = createCompletedOrder(
                otherBuyer,
                List.of(
                        SellerOrder.create(seller, List.of(OrderItem.create(potato, 1)))
                ),
                LocalDateTime.of(2026, 3, 13, 12, 0)
        );
        CheckoutOrder savedOrder = checkoutOrderRepository.saveAndFlush(order);

        Cookie userSession = login("buyer@example.com", "Password12!");

        mockMvc.perform(get("/api/v1/orders/{orderNumber}", savedOrder.getOrderNumber())
                        .cookie(userSession))
                .andExpect(status().isForbidden())
                .andExpect(jsonPath("$.success").value(false))
                .andExpect(jsonPath("$.code").value("AUTH_FORBIDDEN"));
    }

    @Test
    @DisplayName("USER가 존재하지 않는 주문 상세를 조회하면 404를 반환한다")
    void userGetsNotFoundWhenOrderDoesNotExist() throws Exception {
        userRepository.saveAndFlush(User.createUser(
                "buyer@example.com",
                passwordEncoder.encode("Password12!"),
                "구매자"
        ));

        Cookie userSession = login("buyer@example.com", "Password12!");

        mockMvc.perform(get("/api/v1/orders/{orderNumber}", "ORD-UNKNOWNORDERNUMBER0000000000000")
                        .cookie(userSession))
                .andExpect(status().isNotFound())
                .andExpect(jsonPath("$.success").value(false))
                .andExpect(jsonPath("$.code").value("ORDER_NOT_FOUND"));
    }

    @Test
    @DisplayName("SELLER는 내 주문 상세를 조회할 수 없다")
    void sellerCannotGetOwnOrderDetail() throws Exception {
        User seller = userRepository.saveAndFlush(createSeller("seller@example.com", "판매자"));

        Cookie sellerSession = login("seller@example.com", "Password12!");

        mockMvc.perform(get("/api/v1/orders/{orderNumber}", "ORD-SELLERBLOCKED0000000000000000")
                        .cookie(sellerSession))
                .andExpect(status().isForbidden())
                .andExpect(jsonPath("$.code").value("AUTH_FORBIDDEN"));
    }

    @Test
    @DisplayName("ADMIN은 내 주문 상세를 조회할 수 없다")
    void adminCannotGetOwnOrderDetail() throws Exception {
        userRepository.saveAndFlush(User.createAdmin(
                "admin@example.com",
                passwordEncoder.encode("Password12!"),
                "운영자"
        ));

        Cookie adminSession = login("admin@example.com", "Password12!");

        mockMvc.perform(get("/api/v1/orders/{orderNumber}", "ORD-ADMINBLOCKED00000000000000000")
                        .cookie(adminSession))
                .andExpect(status().isForbidden())
                .andExpect(jsonPath("$.code").value("AUTH_FORBIDDEN"));
    }

    @Test
    @DisplayName("미인증 사용자는 내 주문 상세를 조회할 수 없다")
    void unauthenticatedUserCannotGetOwnOrderDetail() throws Exception {
        mockMvc.perform(get("/api/v1/orders/{orderNumber}", "ORD-AUTHBLOCKED00000000000000000"))
                .andExpect(status().isUnauthorized())
                .andExpect(jsonPath("$.code").value("AUTH_UNAUTHORIZED"));
    }

    @Test
    @DisplayName("취소된 주문 상세 조회는 CANCELED 상태와 canceledAt을 반환한다")
    void canceledOrderDetailShowsCanceledState() throws Exception {
        User seller = userRepository.saveAndFlush(createSeller("seller@example.com", "판매자"));
        User buyer = userRepository.saveAndFlush(User.createUser(
                "buyer@example.com",
                passwordEncoder.encode("Password12!"),
                "구매자"
        ));

        Product potato = productRepository.saveAndFlush(createProduct(seller, "감자", 12000L));
        potato.decreaseStock(1);
        potato.restoreStock(1);
        productRepository.saveAndFlush(potato);

        CheckoutOrder canceledOrder = createCanceledOrder(
                buyer,
                List.of(
                        SellerOrder.create(seller, List.of(OrderItem.create(potato, 1)))
                ),
                LocalDateTime.of(2026, 3, 13, 11, 0),
                LocalDateTime.of(2026, 3, 13, 12, 0)
        );
        CheckoutOrder savedOrder = checkoutOrderRepository.saveAndFlush(canceledOrder);

        Cookie userSession = login("buyer@example.com", "Password12!");

        mockMvc.perform(get("/api/v1/orders/{orderNumber}", savedOrder.getOrderNumber())
                        .cookie(userSession))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.data.status").value("CANCELED"))
                .andExpect(jsonPath("$.data.canceledAt").value("2026-03-13T12:00:00"))
                .andExpect(jsonPath("$.data.sellerOrders[0].status").value("CANCELED"))
                .andExpect(jsonPath("$.data.sellerOrders[0].canceledAt").value("2026-03-13T12:00:00"));
    }

    @Test
    @DisplayName("USER는 본인 COMPLETED 주문을 취소할 수 있고 재고가 복구된다")
    void userCanCancelOwnCompletedOrder() throws Exception {
        User seller1 = userRepository.saveAndFlush(createSeller("seller1@example.com", "판매자1"));
        User seller2 = userRepository.saveAndFlush(createSeller("seller2@example.com", "판매자2"));
        User buyer = userRepository.saveAndFlush(User.createUser(
                "buyer@example.com",
                passwordEncoder.encode("Password12!"),
                "구매자"
        ));

        Product potato = productRepository.saveAndFlush(createProduct(seller1, "감자", 12000L));
        Product mackerel = productRepository.saveAndFlush(createProduct(seller2, "고등어", 15000L));
        Product shrimp = productRepository.saveAndFlush(createProduct(seller2, "새우", 7000L, 1));

        potato.decreaseStock(1);
        mackerel.decreaseStock(1);
        shrimp.decreaseStock(1);
        productRepository.saveAndFlush(potato);
        productRepository.saveAndFlush(mackerel);
        productRepository.saveAndFlush(shrimp);

        CheckoutOrder order = createCompletedOrder(
                buyer,
                List.of(
                        SellerOrder.create(seller1, List.of(OrderItem.create(potato, 1))),
                        SellerOrder.create(seller2, List.of(
                                OrderItem.create(mackerel, 1),
                                OrderItem.create(shrimp, 1)
                        ))
                ),
                LocalDateTime.of(2026, 3, 13, 11, 0)
        );
        CheckoutOrder savedOrder = checkoutOrderRepository.saveAndFlush(order);

        Cookie userSession = login("buyer@example.com", "Password12!");
        CsrfSession csrfSession = fetchCsrfSession(userSession);

        mockMvc.perform(post("/api/v1/orders/{orderNumber}/cancel", savedOrder.getOrderNumber())
                        .cookie(userSession)
                        .contentType(MediaType.APPLICATION_JSON)
                        .header(csrfSession.headerName(), csrfSession.token())
                        .content(objectMapper.writeValueAsString(Map.of())))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.success").value(true))
                .andExpect(jsonPath("$.code").value("SUCCESS"))
                .andExpect(jsonPath("$.data.orderNumber").value(orderNumber(savedOrder)))
                .andExpect(jsonPath("$.data.orderId").doesNotExist())
                .andExpect(jsonPath("$.data.status").value("CANCELED"))
                .andExpect(jsonPath("$.data.sellerOrderCount").value(2));

        CheckoutOrder canceledOrder = checkoutOrderRepository.findByIdWithSellerOrders(savedOrder.getId()).orElseThrow();
        List<SellerOrder> sellerOrders = sellerOrderRepository.findAllByCheckoutOrderIdWithItems(savedOrder.getId());

        assertThat(canceledOrder.getStatus()).isEqualTo(OrderStatus.CANCELED);
        assertThat(sellerOrders)
                .extracting(SellerOrder::getStatus)
                .containsOnly(OrderStatus.CANCELED);

        Product restoredPotato = productRepository.findById(potato.getId()).orElseThrow();
        Product restoredMackerel = productRepository.findById(mackerel.getId()).orElseThrow();
        Product restoredShrimp = productRepository.findById(shrimp.getId()).orElseThrow();

        assertThat(restoredPotato.getStockQuantity()).isEqualTo(10);
        assertThat(restoredPotato.getStatus().name()).isEqualTo("ON_SALE");
        assertThat(restoredMackerel.getStockQuantity()).isEqualTo(10);
        assertThat(restoredMackerel.getStatus().name()).isEqualTo("ON_SALE");
        assertThat(restoredShrimp.getStockQuantity()).isEqualTo(1);
        assertThat(restoredShrimp.getStatus().name()).isEqualTo("ON_SALE");
    }

    @Test
    @DisplayName("USER는 타인 주문을 취소할 수 없다")
    void userCannotCancelAnotherUsersOrder() throws Exception {
        User seller = userRepository.saveAndFlush(createSeller("seller@example.com", "판매자"));
        User owner = userRepository.saveAndFlush(User.createUser(
                "owner@example.com",
                passwordEncoder.encode("Password12!"),
                "주문자"
        ));
        userRepository.saveAndFlush(User.createUser(
                "buyer@example.com",
                passwordEncoder.encode("Password12!"),
                "구매자"
        ));

        Product potato = productRepository.saveAndFlush(createProduct(seller, "감자", 12000L));

        potato.decreaseStock(1);
        productRepository.saveAndFlush(potato);

        CheckoutOrder order = createCompletedOrder(
                owner,
                List.of(
                        SellerOrder.create(seller, List.of(OrderItem.create(potato, 1)))
                ),
                LocalDateTime.of(2026, 3, 13, 11, 0)
        );
        CheckoutOrder savedOrder = checkoutOrderRepository.saveAndFlush(order);

        Cookie userSession = login("buyer@example.com", "Password12!");
        CsrfSession csrfSession = fetchCsrfSession(userSession);

        mockMvc.perform(post("/api/v1/orders/{orderNumber}/cancel", savedOrder.getOrderNumber())
                        .cookie(userSession)
                        .contentType(MediaType.APPLICATION_JSON)
                        .header(csrfSession.headerName(), csrfSession.token())
                        .content(objectMapper.writeValueAsString(Map.of())))
                .andExpect(status().isForbidden())
                .andExpect(jsonPath("$.success").value(false))
                .andExpect(jsonPath("$.code").value("AUTH_FORBIDDEN"));
    }

    @Test
    @DisplayName("취소 API는 CSRF 토큰이 없으면 거절된다")
    void cancelRequiresCsrf() throws Exception {
        User seller = userRepository.saveAndFlush(createSeller("seller@example.com", "판매자"));
        User buyer = userRepository.saveAndFlush(User.createUser(
                "buyer@example.com",
                passwordEncoder.encode("Password12!"),
                "구매자"
        ));

        Product potato = productRepository.saveAndFlush(createProduct(seller, "감자", 12000L));

        potato.decreaseStock(1);
        productRepository.saveAndFlush(potato);

        CheckoutOrder order = createCompletedOrder(
                buyer,
                List.of(
                        SellerOrder.create(seller, List.of(OrderItem.create(potato, 1)))
                ),
                LocalDateTime.of(2026, 3, 13, 11, 0)
        );
        CheckoutOrder savedOrder = checkoutOrderRepository.saveAndFlush(order);

        Cookie userSession = login("buyer@example.com", "Password12!");

        mockMvc.perform(post("/api/v1/orders/{orderNumber}/cancel", savedOrder.getOrderNumber())
                        .cookie(userSession)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(Map.of())))
                .andExpect(status().isForbidden())
                .andExpect(jsonPath("$.code").value("AUTH_CSRF_INVALID"));
    }

    @Test
    @DisplayName("미인증 사용자는 주문 취소 API에 접근할 수 없다")
    void unauthenticatedUserCannotCancelOrder() throws Exception {
        CsrfSession anonymousCsrfSession = fetchCsrfSession();

        mockMvc.perform(post("/api/v1/orders/{orderNumber}/cancel", "ORD-UNAUTHCANCEL0000000000000000")
                        .cookie(anonymousCsrfSession.sessionCookie())
                        .contentType(MediaType.APPLICATION_JSON)
                        .header(anonymousCsrfSession.headerName(), anonymousCsrfSession.token())
                        .content(objectMapper.writeValueAsString(Map.of())))
                .andExpect(status().isUnauthorized())
                .andExpect(jsonPath("$.code").value("AUTH_UNAUTHORIZED"));
    }

    @Test
    @DisplayName("SELLER는 주문 취소 API에 접근할 수 없다")
    void sellerCannotCancelOrder() throws Exception {
        userRepository.saveAndFlush(createSeller("seller@example.com", "판매자"));

        Cookie sellerSession = login("seller@example.com", "Password12!");
        CsrfSession csrfSession = fetchCsrfSession(sellerSession);

        mockMvc.perform(post("/api/v1/orders/{orderNumber}/cancel", "ORD-SELLERCANCEL0000000000000000")
                        .cookie(sellerSession)
                        .contentType(MediaType.APPLICATION_JSON)
                        .header(csrfSession.headerName(), csrfSession.token())
                        .content(objectMapper.writeValueAsString(Map.of())))
                .andExpect(status().isForbidden())
                .andExpect(jsonPath("$.code").value("AUTH_FORBIDDEN"));
    }

    @Test
    @DisplayName("ADMIN은 주문 취소 API에 접근할 수 없다")
    void adminCannotCancelOrder() throws Exception {
        userRepository.saveAndFlush(User.createAdmin(
                "admin@example.com",
                passwordEncoder.encode("Password12!"),
                "운영자"
        ));

        Cookie adminSession = login("admin@example.com", "Password12!");
        CsrfSession csrfSession = fetchCsrfSession(adminSession);

        mockMvc.perform(post("/api/v1/orders/{orderNumber}/cancel", "ORD-ADMINCANCEL00000000000000000")
                        .cookie(adminSession)
                        .contentType(MediaType.APPLICATION_JSON)
                        .header(csrfSession.headerName(), csrfSession.token())
                        .content(objectMapper.writeValueAsString(Map.of())))
                .andExpect(status().isForbidden())
                .andExpect(jsonPath("$.code").value("AUTH_FORBIDDEN"));
    }

    @Test
    @DisplayName("CREATED 상태 주문은 취소할 수 없다")
    void cancelRejectsCreatedOrder() throws Exception {
        User seller = userRepository.saveAndFlush(createSeller("seller@example.com", "판매자"));
        User buyer = userRepository.saveAndFlush(User.createUser(
                "buyer@example.com",
                passwordEncoder.encode("Password12!"),
                "구매자"
        ));

        Product potato = productRepository.saveAndFlush(createProduct(seller, "감자", 12000L));
        CheckoutOrder createdOrder = checkoutOrderRepository.saveAndFlush(CheckoutOrder.create(
                buyer,
                List.of(
                        SellerOrder.create(seller, List.of(OrderItem.create(potato, 1)))
                )
        ));

        Cookie userSession = login("buyer@example.com", "Password12!");
        CsrfSession csrfSession = fetchCsrfSession(userSession);

        mockMvc.perform(post("/api/v1/orders/{orderNumber}/cancel", createdOrder.getOrderNumber())
                        .cookie(userSession)
                        .contentType(MediaType.APPLICATION_JSON)
                        .header(csrfSession.headerName(), csrfSession.token())
                        .content(objectMapper.writeValueAsString(Map.of())))
                .andExpect(status().isConflict())
                .andExpect(jsonPath("$.code").value("ORDER_STATUS_TRANSITION_INVALID"));
    }

    @Test
    @DisplayName("이미 취소된 주문은 다시 취소할 수 없다")
    void cancelRejectsCanceledOrder() throws Exception {
        User seller = userRepository.saveAndFlush(createSeller("seller@example.com", "판매자"));
        User buyer = userRepository.saveAndFlush(User.createUser(
                "buyer@example.com",
                passwordEncoder.encode("Password12!"),
                "구매자"
        ));

        Product potato = productRepository.saveAndFlush(createProduct(seller, "감자", 12000L));
        potato.decreaseStock(1);
        potato.restoreStock(1);
        productRepository.saveAndFlush(potato);

        CheckoutOrder canceledOrder = checkoutOrderRepository.saveAndFlush(createCanceledOrder(
                buyer,
                List.of(
                        SellerOrder.create(seller, List.of(OrderItem.create(potato, 1)))
                ),
                LocalDateTime.of(2026, 3, 13, 11, 0),
                LocalDateTime.of(2026, 3, 13, 12, 0)
        ));

        Cookie userSession = login("buyer@example.com", "Password12!");
        CsrfSession csrfSession = fetchCsrfSession(userSession);

        mockMvc.perform(post("/api/v1/orders/{orderNumber}/cancel", canceledOrder.getOrderNumber())
                        .cookie(userSession)
                        .contentType(MediaType.APPLICATION_JSON)
                        .header(csrfSession.headerName(), csrfSession.token())
                        .content(objectMapper.writeValueAsString(Map.of())))
                .andExpect(status().isConflict())
                .andExpect(jsonPath("$.code").value("ORDER_STATUS_TRANSITION_INVALID"));
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

    private CheckoutOrder createCanceledOrder(
            User buyer,
            List<SellerOrder> sellerOrders,
            LocalDateTime completedAt,
            LocalDateTime canceledAt
    ) {
        CheckoutOrder checkoutOrder = createCompletedOrder(buyer, sellerOrders, completedAt);
        checkoutOrder.cancel(canceledAt);
        return checkoutOrder;
    }

    private Product createProduct(User seller, String name, Long price) {
        return createProduct(seller, name, price, 10);
    }

    private Product createProduct(User seller, String name, Long price, int stockQuantity) {
        return Product.create(
                seller,
                name,
                name + " 상품 설명",
                ProductCategory.AGRICULTURE,
                price,
                stockQuantity,
                "https://cdn.example.com/" + name + ".jpg"
        );
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

    private String orderNumber(CheckoutOrder checkoutOrder) {
        return (String) ReflectionTestUtils.getField(checkoutOrder, "orderNumber");
    }

    private Cookie login(String email, String password) throws Exception {
        CsrfSession anonymousCsrfSession = fetchCsrfSession();

        MvcResult result = mockMvc.perform(org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post("/api/v1/auth/login")
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

    private CsrfSession fetchCsrfSession(Cookie sessionCookie) throws Exception {
        MvcResult result = mockMvc.perform(get("/api/v1/csrf")
                        .cookie(sessionCookie))
                .andExpect(status().isOk())
                .andReturn();

        var data = objectMapper.readTree(result.getResponse().getContentAsString()).get("data");
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
