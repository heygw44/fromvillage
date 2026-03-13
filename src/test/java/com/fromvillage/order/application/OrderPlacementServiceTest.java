package com.fromvillage.order.application;

import com.fromvillage.common.exception.BusinessException;
import com.fromvillage.common.exception.ErrorCode;
import com.fromvillage.order.domain.CheckoutOrder;
import com.fromvillage.order.domain.CheckoutOrderStore;
import com.fromvillage.product.domain.Product;
import com.fromvillage.product.domain.ProductCategory;
import com.fromvillage.product.domain.ProductStatus;
import com.fromvillage.user.domain.User;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.test.util.ReflectionTestUtils;

import java.time.Clock;
import java.time.Instant;
import java.time.LocalDateTime;
import java.time.ZoneOffset;
import java.util.List;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.BDDMockito.given;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;

@ExtendWith(MockitoExtension.class)
class OrderPlacementServiceTest {

    @Mock
    private CheckoutOrderStore checkoutOrderStore;

    private final Clock clock = Clock.fixed(Instant.parse("2026-03-12T03:00:00Z"), ZoneOffset.UTC);

    private OrderPlacementService orderPlacementService;

    @BeforeEach
    void setUp() {
        orderPlacementService = new OrderPlacementService(checkoutOrderStore, clock);
    }

    @Test
    @DisplayName("같은 판매자 id를 가진 다른 엔티티 인스턴스라도 하나의 seller order로 묶는다")
    void placeGroupsOrderLinesBySellerId() {
        User buyer = user(100L, "buyer@example.com", "구매자");
        User sellerInstance1 = seller(1L, "seller1@example.com", "판매자");
        User sellerInstance2 = seller(1L, "seller2@example.com", "판매자");

        Product potato = Product.create(
                sellerInstance1,
                "유기농 감자 5kg",
                "해남 햇감자",
                ProductCategory.AGRICULTURE,
                22000L,
                10,
                "https://cdn.example.com/potato.jpg"
        );
        Product cabbage = Product.create(
                sellerInstance2,
                "절임배추 10kg",
                "전남 해남 절임배추",
                ProductCategory.AGRICULTURE,
                18000L,
                8,
                "https://cdn.example.com/cabbage.jpg"
        );

        given(checkoutOrderStore.save(org.mockito.ArgumentMatchers.any(CheckoutOrder.class)))
                .willAnswer(invocation -> invocation.getArgument(0));

        CheckoutOrder result = orderPlacementService.place(
                buyer,
                List.of(
                        OrderCheckoutLine.of(potato, 2),
                        OrderCheckoutLine.of(cabbage, 1)
                )
        );

        assertThat(result.getSellerOrders()).hasSize(1);
        assertThat(result.getTotalAmount()).isEqualTo(62000L);
        assertThat(result.getFinalAmount()).isEqualTo(62000L);
        assertThat(result.getStatus().name()).isEqualTo("COMPLETED");
    }

    @Test
    @DisplayName("주문으로 재고가 0이 되면 상품 상태는 SOLD_OUT으로 바뀐다")
    void placeMarksProductSoldOutWhenStockBecomesZero() {
        User buyer = user(100L, "buyer@example.com", "구매자");
        User seller = seller(1L, "seller@example.com", "판매자");

        Product mackerel = Product.create(
                seller,
                "손질 고등어",
                "당일 손질 고등어",
                ProductCategory.FISHERY,
                15000L,
                3,
                "https://cdn.example.com/mackerel.jpg"
        );

        given(checkoutOrderStore.save(org.mockito.ArgumentMatchers.any(CheckoutOrder.class)))
                .willAnswer(invocation -> invocation.getArgument(0));

        CheckoutOrder result = orderPlacementService.place(
                buyer,
                List.of(OrderCheckoutLine.of(mackerel, 3))
        );

        assertThat(result.getStatus().name()).isEqualTo("COMPLETED");
        assertThat(mackerel.getStockQuantity()).isEqualTo(0);
        assertThat(mackerel.getStatus()).isEqualTo(ProductStatus.SOLD_OUT);
    }

    @Test
    @DisplayName("주문 라인이 비어 있으면 ORDER_ITEMS_REQUIRED 예외가 발생한다")
    void placeRejectsEmptyOrderLines() {
        User buyer = user(100L, "buyer@example.com", "구매자");

        assertThatThrownBy(() -> orderPlacementService.place(buyer, List.of()))
                .isInstanceOf(BusinessException.class)
                .extracting("errorCode")
                .isEqualTo(ErrorCode.ORDER_ITEMS_REQUIRED);

        verify(checkoutOrderStore, never()).save(org.mockito.ArgumentMatchers.any(CheckoutOrder.class));
    }

    @Test
    @DisplayName("판매 불가 상품이 포함되면 ORDER_PRODUCT_UNAVAILABLE 예외가 발생한다")
    void placeRejectsUnavailableProduct() {
        User buyer = user(100L, "buyer@example.com", "구매자");
        User seller = seller(1L, "seller@example.com", "판매자");
        Product cabbage = Product.create(
                seller,
                "절임배추 10kg",
                "전남 해남 절임배추",
                ProductCategory.AGRICULTURE,
                18000L,
                3,
                "https://cdn.example.com/cabbage.jpg"
        );
        cabbage.decreaseStock(3);

        assertThatThrownBy(() -> orderPlacementService.place(
                buyer,
                List.of(OrderCheckoutLine.of(cabbage, 1))
        ))
                .isInstanceOf(BusinessException.class)
                .extracting("errorCode")
                .isEqualTo(ErrorCode.ORDER_PRODUCT_UNAVAILABLE);

        verify(checkoutOrderStore, never()).save(org.mockito.ArgumentMatchers.any(CheckoutOrder.class));
    }

    @Test
    @DisplayName("재고가 부족하면 PRODUCT_STOCK_INSUFFICIENT 예외가 발생한다")
    void placeRejectsInsufficientStock() {
        User buyer = user(100L, "buyer@example.com", "구매자");
        User seller = seller(1L, "seller@example.com", "판매자");
        Product potato = Product.create(
                seller,
                "유기농 감자 5kg",
                "해남 햇감자",
                ProductCategory.AGRICULTURE,
                22000L,
                2,
                "https://cdn.example.com/potato.jpg"
        );

        assertThatThrownBy(() -> orderPlacementService.place(
                buyer,
                List.of(OrderCheckoutLine.of(potato, 3))
        ))
                .isInstanceOf(BusinessException.class)
                .extracting("errorCode")
                .isEqualTo(ErrorCode.PRODUCT_STOCK_INSUFFICIENT);

        verify(checkoutOrderStore, never()).save(org.mockito.ArgumentMatchers.any(CheckoutOrder.class));
    }

    private User user(Long id, String email, String nickname) {
        User user = User.createUser(email, "encoded-password", nickname);
        ReflectionTestUtils.setField(user, "id", id);
        return user;
    }

    private User seller(Long id, String email, String nickname) {
        User seller = User.createUser(email, "encoded-password", nickname);
        seller.approveSeller(LocalDateTime.of(2026, 3, 12, 12, 0));
        ReflectionTestUtils.setField(seller, "id", id);
        return seller;
    }
}
