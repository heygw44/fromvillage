package com.fromvillage.order.domain;

import com.fromvillage.common.exception.BusinessException;
import com.fromvillage.common.exception.ErrorCode;
import com.fromvillage.product.domain.Product;
import com.fromvillage.product.domain.ProductCategory;
import com.fromvillage.user.domain.User;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

import java.time.LocalDateTime;
import java.util.List;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

class CheckoutOrderTest {

    @Test
    @DisplayName("체크아웃 주문을 생성할 수 있다")
    void createCheckoutOrder() {
        User buyer = User.createUser("buyer@example.com", "encoded-password", "buyer");

        User seller1 = createSeller("seller1@example.com", "seller1");
        User seller2 = createSeller("seller2@example.com", "seller2");

        SellerOrder firstSellerOrder = SellerOrder.create(
                seller1,
                List.of(OrderItem.create(createProduct(seller1, "감자", 12000L), 2))
        );
        SellerOrder secondSellerOrder = SellerOrder.create(
                seller2,
                List.of(OrderItem.create(createProduct(seller2, "배추", 8000L), 1))
        );

        CheckoutOrder checkoutOrder = CheckoutOrder.create(
                buyer,
                List.of(firstSellerOrder, secondSellerOrder)
        );

        assertThat(checkoutOrder.getUser()).isEqualTo(buyer);
        assertThat(checkoutOrder.getStatus()).isEqualTo(OrderStatus.CREATED);
        assertThat(checkoutOrder.getSellerOrders()).hasSize(2);
        assertThat(checkoutOrder.getTotalAmount()).isEqualTo(32000L);
        assertThat(checkoutOrder.getDiscountAmount()).isEqualTo(0L);
        assertThat(checkoutOrder.getFinalAmount()).isEqualTo(32000L);
        assertThat(checkoutOrder.getCompletedAt()).isNull();
        assertThat(checkoutOrder.getCanceledAt()).isNull();
    }

    @Test
    @DisplayName("체크아웃 주문 완료 시 하위 판매자 주문도 함께 완료된다")
    void completeCheckoutOrder() {
        User buyer = User.createUser("buyer@example.com", "encoded-password", "buyer");
        User seller1 = createSeller("seller1@example.com", "seller1");
        User seller2 = createSeller("seller2@example.com", "seller2");

        SellerOrder firstSellerOrder = SellerOrder.create(
                seller1,
                List.of(OrderItem.create(createProduct(seller1, "감자", 12000L), 2))
        );
        SellerOrder secondSellerOrder = SellerOrder.create(
                seller2,
                List.of(OrderItem.create(createProduct(seller2, "배추", 8000L), 1))
        );

        CheckoutOrder checkoutOrder = CheckoutOrder.create(
                buyer,
                List.of(firstSellerOrder, secondSellerOrder)
        );

        LocalDateTime completedAt = LocalDateTime.of(2026, 3, 12, 12, 0);

        checkoutOrder.complete(completedAt);

        assertThat(checkoutOrder.getStatus()).isEqualTo(OrderStatus.COMPLETED);
        assertThat(checkoutOrder.getCompletedAt()).isEqualTo(completedAt);
        assertThat(checkoutOrder.getSellerOrders())
                .extracting(SellerOrder::getStatus)
                .containsOnly(OrderStatus.COMPLETED);
    }

    @Test
    @DisplayName("완료된 체크아웃 주문 취소 시 하위 판매자 주문도 함께 취소된다")
    void cancelCheckoutOrderCascadesSellerOrders() {
        User buyer = User.createUser("buyer@example.com", "encoded-password", "buyer");
        User seller1 = createSeller("seller1@example.com", "seller1");
        User seller2 = createSeller("seller2@example.com", "seller2");

        SellerOrder firstSellerOrder = SellerOrder.create(
                seller1,
                List.of(OrderItem.create(createProduct(seller1, "감자", 12000L), 2))
        );
        SellerOrder secondSellerOrder = SellerOrder.create(
                seller2,
                List.of(OrderItem.create(createProduct(seller2, "배추", 8000L), 1))
        );

        CheckoutOrder checkoutOrder = CheckoutOrder.create(
                buyer,
                List.of(firstSellerOrder, secondSellerOrder)
        );
        checkoutOrder.complete(LocalDateTime.of(2026, 3, 12, 12, 0));

        LocalDateTime canceledAt = LocalDateTime.of(2026, 3, 12, 13, 0);

        checkoutOrder.cancel(canceledAt);

        assertThat(checkoutOrder.getStatus()).isEqualTo(OrderStatus.CANCELED);
        assertThat(checkoutOrder.getCanceledAt()).isEqualTo(canceledAt);
        assertThat(checkoutOrder.getSellerOrders())
                .extracting(SellerOrder::getStatus)
                .containsOnly(OrderStatus.CANCELED);
    }

    @Test
    @DisplayName("판매자 주문 없이 체크아웃 주문을 생성할 수 없다")
    void createCheckoutOrderRejectsEmptySellerOrders() {
        User buyer = User.createUser("buyer@example.com", "encoded-password", "buyer");

        assertThatThrownBy(() -> CheckoutOrder.create(buyer, List.of()))
                .isInstanceOf(BusinessException.class)
                .extracting(exception -> ((BusinessException) exception).getErrorCode())
                .isEqualTo(ErrorCode.ORDER_SELLER_ORDERS_REQUIRED);
    }

    @Test
    @DisplayName("이미 다른 체크아웃 주문에 속한 판매자 주문은 재사용할 수 없다")
    void createCheckoutOrderRejectsReassigningSellerOrder() {
        User buyer = User.createUser("buyer@example.com", "encoded-password", "buyer");
        User anotherBuyer = User.createUser("another-buyer@example.com", "encoded-password", "anotherBuyer");
        User seller = createSeller("seller@example.com", "seller");

        SellerOrder sellerOrder = SellerOrder.create(
                seller,
                List.of(OrderItem.create(createProduct(seller, "감자", 12000L), 2))
        );

        CheckoutOrder.create(buyer, List.of(sellerOrder));

        assertThatThrownBy(() -> CheckoutOrder.create(anotherBuyer, List.of(sellerOrder)))
                .isInstanceOf(IllegalStateException.class)
                .hasMessage("Seller order is already assigned to another checkout order.");
    }

    @Test
    @DisplayName("생성 상태가 아닌 체크아웃 주문 완료는 허용하지 않는다")
    void completeCheckoutOrderRejectsInvalidTransition() {
        User buyer = User.createUser("buyer@example.com", "encoded-password", "buyer");
        User seller = createSeller("seller@example.com", "seller");

        CheckoutOrder checkoutOrder = CheckoutOrder.create(
                buyer,
                List.of(SellerOrder.create(
                        seller,
                        List.of(OrderItem.create(createProduct(seller, "감자", 12000L), 2))
                ))
        );
        checkoutOrder.complete(LocalDateTime.of(2026, 3, 12, 12, 0));

        assertThatThrownBy(() -> checkoutOrder.complete(LocalDateTime.of(2026, 3, 12, 13, 0)))
                .isInstanceOf(BusinessException.class)
                .extracting(exception -> ((BusinessException) exception).getErrorCode())
                .isEqualTo(ErrorCode.ORDER_STATUS_TRANSITION_INVALID);
    }

    @Test
    @DisplayName("완료 상태가 아닌 체크아웃 주문 취소는 허용하지 않는다")
    void cancelCheckoutOrderRejectsInvalidTransition() {
        User buyer = User.createUser("buyer@example.com", "encoded-password", "buyer");
        User seller = createSeller("seller@example.com", "seller");

        CheckoutOrder checkoutOrder = CheckoutOrder.create(
                buyer,
                List.of(SellerOrder.create(
                        seller,
                        List.of(OrderItem.create(createProduct(seller, "감자", 12000L), 2))
                ))
        );

        assertThatThrownBy(() -> checkoutOrder.cancel(LocalDateTime.of(2026, 3, 12, 13, 0)))
                .isInstanceOf(BusinessException.class)
                .extracting(exception -> ((BusinessException) exception).getErrorCode())
                .isEqualTo(ErrorCode.ORDER_STATUS_TRANSITION_INVALID);
    }

    private User createSeller(String email, String nickname) {
        User seller = User.createUser(email, "encoded-password", nickname);
        seller.approveSeller(LocalDateTime.of(2026, 3, 12, 9, 0));
        return seller;
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
}
