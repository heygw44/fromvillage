package com.fromvillage.order.application;

import com.fromvillage.common.exception.BusinessException;
import com.fromvillage.common.exception.ErrorCode;
import com.fromvillage.order.domain.CheckoutOrder;
import com.fromvillage.order.domain.CheckoutOrderQueryPort;
import com.fromvillage.order.domain.OrderItem;
import com.fromvillage.order.domain.OrderStatus;
import com.fromvillage.order.domain.SellerOrder;
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
import java.util.Optional;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.BDDMockito.given;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;

@ExtendWith(MockitoExtension.class)
class OrderCancelServiceTest {

    @Mock
    private CheckoutOrderQueryPort checkoutOrderQueryPort;

    private Clock clock;
    private OrderCancelService orderCancelService;

    @BeforeEach
    void setUp() {
        clock = Clock.fixed(Instant.parse("2026-03-13T03:00:00Z"), ZoneOffset.UTC);
        orderCancelService = new OrderCancelService(checkoutOrderQueryPort, clock);
    }

    @Test
    @DisplayName("본인 COMPLETED 주문을 취소하면 하위 주문이 함께 취소되고 재고가 복구된다")
    void cancelCompletedOrderRestoresStocks() {
        User buyer = user(100L, "buyer@example.com", "구매자");
        User seller1 = seller(1L, "seller1@example.com", "판매자1");
        User seller2 = seller(2L, "seller2@example.com", "판매자2");

        Product potato = product(seller1, "감자", 12000L, 2);
        Product mackerel = product(seller2, "고등어", 15000L, 1);

        potato.decreaseStock(2);
        mackerel.decreaseStock(1);

        CheckoutOrder checkoutOrder = completedOrder(
                "ORD-20000000000000000000000000000000",
                200L,
                buyer,
                List.of(
                        SellerOrder.create(seller1, List.of(OrderItem.create(potato, 2))),
                        SellerOrder.create(seller2, List.of(OrderItem.create(mackerel, 1)))
                ),
                LocalDateTime.of(2026, 3, 13, 10, 0)
        );

        given(checkoutOrderQueryPort.findOwnerIdByOrderNumber(checkoutOrder.getOrderNumber()))
                .willReturn(Optional.of(100L));
        given(checkoutOrderQueryPort.findDetailByOrderNumber(checkoutOrder.getOrderNumber()))
                .willReturn(Optional.of(checkoutOrder));

        OrderSummary result = orderCancelService.cancel(100L, checkoutOrder.getOrderNumber());

        assertThat(result.orderNumber()).isEqualTo(checkoutOrder.getOrderNumber());
        assertThat(result.status()).isEqualTo(OrderStatus.CANCELED);
        assertThat(result.sellerOrderCount()).isEqualTo(2);
        assertThat(result.canceledAt()).isEqualTo(LocalDateTime.of(2026, 3, 13, 3, 0));

        assertThat(checkoutOrder.getStatus()).isEqualTo(OrderStatus.CANCELED);
        assertThat(checkoutOrder.getCanceledAt()).isEqualTo(LocalDateTime.of(2026, 3, 13, 3, 0));
        assertThat(checkoutOrder.getSellerOrders())
                .extracting(SellerOrder::getStatus)
                .containsOnly(OrderStatus.CANCELED);

        assertThat(potato.getStockQuantity()).isEqualTo(2);
        assertThat(potato.getStatus()).isEqualTo(ProductStatus.ON_SALE);
        assertThat(mackerel.getStockQuantity()).isEqualTo(1);
        assertThat(mackerel.getStatus()).isEqualTo(ProductStatus.ON_SALE);
    }

    @Test
    @DisplayName("타인 주문은 취소할 수 없다")
    void cancelRejectsAnotherUsersOrder() {
        User owner = user(101L, "owner@example.com", "주문자");
        User requester = user(100L, "buyer@example.com", "구매자");
        User seller = seller(1L, "seller@example.com", "판매자");

        Product potato = product(seller, "감자", 12000L, 3);
        potato.decreaseStock(1);

        CheckoutOrder checkoutOrder = completedOrder(
                "ORD-20000000000000000000000000000000",
                200L,
                owner,
                List.of(
                        SellerOrder.create(seller, List.of(OrderItem.create(potato, 1)))
                ),
                LocalDateTime.of(2026, 3, 13, 10, 0)
        );

        given(checkoutOrderQueryPort.findOwnerIdByOrderNumber(checkoutOrder.getOrderNumber()))
                .willReturn(Optional.of(101L));

        assertThatThrownBy(() -> orderCancelService.cancel(100L, checkoutOrder.getOrderNumber()))
                .isInstanceOf(BusinessException.class)
                .extracting("errorCode")
                .isEqualTo(ErrorCode.AUTH_FORBIDDEN);

        verify(checkoutOrderQueryPort, never()).findDetailByOrderNumber(anyString());
    }

    @Test
    @DisplayName("존재하지 않는 주문은 취소할 수 없다")
    void cancelRejectsMissingOrder() {
        String orderNumber = "ORD-99900000000000000000000000000000";
        given(checkoutOrderQueryPort.findOwnerIdByOrderNumber(orderNumber)).willReturn(Optional.empty());

        assertThatThrownBy(() -> orderCancelService.cancel(100L, orderNumber))
                .isInstanceOf(BusinessException.class)
                .extracting("errorCode")
                .isEqualTo(ErrorCode.ORDER_NOT_FOUND);

        verify(checkoutOrderQueryPort, never()).findDetailByOrderNumber(anyString());
    }

    @Test
    @DisplayName("COMPLETED 상태가 아닌 주문은 취소할 수 없다")
    void cancelRejectsInvalidOrderStatus() {
        User buyer = user(100L, "buyer@example.com", "구매자");
        User seller = seller(1L, "seller@example.com", "판매자");

        Product potato = product(seller, "감자", 12000L, 3);

        CheckoutOrder checkoutOrder = CheckoutOrder.create(
                buyer,
                List.of(
                        SellerOrder.create(seller, List.of(OrderItem.create(potato, 1)))
                )
        );
        ReflectionTestUtils.setField(checkoutOrder, "orderNumber", "ORD-20000000000000000000000000000000");
        ReflectionTestUtils.setField(checkoutOrder, "id", 200L);

        given(checkoutOrderQueryPort.findOwnerIdByOrderNumber(checkoutOrder.getOrderNumber()))
                .willReturn(Optional.of(100L));
        given(checkoutOrderQueryPort.findDetailByOrderNumber(checkoutOrder.getOrderNumber()))
                .willReturn(Optional.of(checkoutOrder));

        assertThatThrownBy(() -> orderCancelService.cancel(100L, checkoutOrder.getOrderNumber()))
                .isInstanceOf(BusinessException.class)
                .extracting("errorCode")
                .isEqualTo(ErrorCode.ORDER_STATUS_TRANSITION_INVALID);
    }

    @Test
    @DisplayName("이미 취소된 주문은 다시 취소할 수 없고 재고도 다시 복구되지 않는다")
    void cancelRejectsAlreadyCanceledOrderWithoutRestoringAgain() {
        User buyer = user(100L, "buyer@example.com", "구매자");
        User seller = seller(1L, "seller@example.com", "판매자");

        Product potato = product(seller, "감자", 12000L, 3);
        potato.decreaseStock(1);
        potato.restoreStock(1);

        CheckoutOrder checkoutOrder = completedOrder(
                "ORD-20000000000000000000000000000000",
                200L,
                buyer,
                List.of(
                        SellerOrder.create(seller, List.of(OrderItem.create(potato, 1)))
                ),
                LocalDateTime.of(2026, 3, 13, 10, 0)
        );
        checkoutOrder.cancel(LocalDateTime.of(2026, 3, 13, 11, 0));

        given(checkoutOrderQueryPort.findOwnerIdByOrderNumber(checkoutOrder.getOrderNumber()))
                .willReturn(Optional.of(100L));
        given(checkoutOrderQueryPort.findDetailByOrderNumber(checkoutOrder.getOrderNumber()))
                .willReturn(Optional.of(checkoutOrder));

        assertThatThrownBy(() -> orderCancelService.cancel(100L, checkoutOrder.getOrderNumber()))
                .isInstanceOf(BusinessException.class)
                .extracting("errorCode")
                .isEqualTo(ErrorCode.ORDER_STATUS_TRANSITION_INVALID);

        assertThat(potato.getStockQuantity()).isEqualTo(3);
        assertThat(potato.getStatus()).isEqualTo(ProductStatus.ON_SALE);
    }

    private CheckoutOrder completedOrder(
            String orderNumber,
            Long orderId,
            User buyer,
            List<SellerOrder> sellerOrders,
            LocalDateTime completedAt
    ) {
        CheckoutOrder checkoutOrder = CheckoutOrder.create(buyer, sellerOrders);
        ReflectionTestUtils.setField(checkoutOrder, "orderNumber", orderNumber);
        ReflectionTestUtils.setField(checkoutOrder, "id", orderId);
        checkoutOrder.complete(completedAt);
        return checkoutOrder;
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

    private Product product(User seller, String name, Long price, int stockQuantity) {
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
}
