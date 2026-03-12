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

class SellerOrderTest {

    @Test
    @DisplayName("판매자 주문을 생성할 수 있다")
    void createSellerOrder() {
        User seller = createSeller("seller@example.com", "seller");

        OrderItem firstItem = OrderItem.create(createProduct(seller, "감자", 12000L), 2);
        OrderItem secondItem = OrderItem.create(createProduct(seller, "배추", 8000L), 1);

        SellerOrder sellerOrder = SellerOrder.create(seller, List.of(firstItem, secondItem));

        assertThat(sellerOrder.getSeller()).isEqualTo(seller);
        assertThat(sellerOrder.getStatus()).isEqualTo(OrderStatus.CREATED);
        assertThat(sellerOrder.getOrderItems()).hasSize(2);
        assertThat(sellerOrder.getTotalAmount()).isEqualTo(32000L);
        assertThat(sellerOrder.getDiscountAmount()).isEqualTo(0L);
        assertThat(sellerOrder.getFinalAmount()).isEqualTo(32000L);
        assertThat(sellerOrder.getCompletedAt()).isNull();
        assertThat(sellerOrder.getCanceledAt()).isNull();
    }

    @Test
    @DisplayName("판매자 주문을 완료할 수 있다")
    void completeSellerOrder() {
        User seller = createSeller("seller@example.com", "seller");
        SellerOrder sellerOrder = SellerOrder.create(
                seller,
                List.of(OrderItem.create(createProduct(seller, "감자", 12000L), 2))
        );

        LocalDateTime completedAt = LocalDateTime.of(2026, 3, 12, 10, 0);

        sellerOrder.complete(completedAt);

        assertThat(sellerOrder.getStatus()).isEqualTo(OrderStatus.COMPLETED);
        assertThat(sellerOrder.getCompletedAt()).isEqualTo(completedAt);
    }

    @Test
    @DisplayName("완료된 판매자 주문을 취소할 수 있다")
    void cancelCompletedSellerOrder() {
        User seller = createSeller("seller@example.com", "seller");
        SellerOrder sellerOrder = SellerOrder.create(
                seller,
                List.of(OrderItem.create(createProduct(seller, "감자", 12000L), 2))
        );
        sellerOrder.complete(LocalDateTime.of(2026, 3, 12, 10, 0));

        LocalDateTime canceledAt = LocalDateTime.of(2026, 3, 12, 11, 0);

        sellerOrder.cancel(canceledAt);

        assertThat(sellerOrder.getStatus()).isEqualTo(OrderStatus.CANCELED);
        assertThat(sellerOrder.getCanceledAt()).isEqualTo(canceledAt);
    }

    @Test
    @DisplayName("생성 상태가 아닌 주문 완료는 허용하지 않는다")
    void completeSellerOrderRejectsInvalidTransition() {
        User seller = createSeller("seller@example.com", "seller");
        SellerOrder sellerOrder = SellerOrder.create(
                seller,
                List.of(OrderItem.create(createProduct(seller, "감자", 12000L), 2))
        );
        sellerOrder.complete(LocalDateTime.of(2026, 3, 12, 10, 0));

        assertThatThrownBy(() -> sellerOrder.complete(LocalDateTime.of(2026, 3, 12, 11, 0)))
                .isInstanceOf(BusinessException.class)
                .extracting(exception -> ((BusinessException) exception).getErrorCode())
                .isEqualTo(ErrorCode.ORDER_STATUS_TRANSITION_INVALID);
    }

    @Test
    @DisplayName("완료 상태가 아닌 주문 취소는 허용하지 않는다")
    void cancelSellerOrderRejectsInvalidTransition() {
        User seller = createSeller("seller@example.com", "seller");
        SellerOrder sellerOrder = SellerOrder.create(
                seller,
                List.of(OrderItem.create(createProduct(seller, "감자", 12000L), 2))
        );

        assertThatThrownBy(() -> sellerOrder.cancel(LocalDateTime.of(2026, 3, 12, 11, 0)))
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
