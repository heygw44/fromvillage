package com.fromvillage.order.domain;

import com.fromvillage.common.exception.BusinessException;
import com.fromvillage.common.exception.ErrorCode;
import com.fromvillage.product.domain.Product;
import com.fromvillage.product.domain.ProductCategory;
import com.fromvillage.user.domain.User;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

import java.time.LocalDateTime;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

class OrderItemTest {

    @Test
    @DisplayName("주문 아이템을 생성할 수 있다")
    void createOrderItem() {
        User seller = createSeller("seller@example.com", "seller");
        Product product = Product.create(
                seller,
                "감자",
                "해남 햇감자",
                ProductCategory.AGRICULTURE,
                12000L,
                10,
                "https://cdn.example.com/potato.jpg"
        );

        OrderItem orderItem = OrderItem.create(product, 2);

        assertThat(orderItem.getProduct()).isEqualTo(product);
        assertThat(orderItem.getProductNameSnapshot()).isEqualTo("감자");
        assertThat(orderItem.getProductPriceSnapshot()).isEqualTo(12000L);
        assertThat(orderItem.getQuantity()).isEqualTo(2);
        assertThat(orderItem.getLineAmount()).isEqualTo(24000L);
    }

    @Test
    @DisplayName("주문 수량은 1 이상이어야 한다")
    void createOrderItemRejectsInvalidQuantity() {
        User seller = createSeller("seller@example.com", "seller");
        Product product = Product.create(
                seller,
                "감자",
                "해남 햇감자",
                ProductCategory.AGRICULTURE,
                12000L,
                10,
                "https://cdn.example.com/potato.jpg"
        );

        assertThatThrownBy(() -> OrderItem.create(product, 0))
                .isInstanceOf(BusinessException.class)
                .extracting(exception -> ((BusinessException) exception).getErrorCode())
                .isEqualTo(ErrorCode.ORDER_ITEM_QUANTITY_INVALID);
    }

    @Test
    @DisplayName("주문 아이템은 주문 당시 상품명과 가격을 스냅샷으로 저장한다")
    void createOrderItemStoresSnapshot() {
        User seller = createSeller("seller@example.com", "seller");
        Product product = Product.create(
                seller,
                "감자",
                "해남 햇감자",
                ProductCategory.AGRICULTURE,
                12000L,
                10,
                "https://cdn.example.com/potato.jpg"
        );

        OrderItem orderItem = OrderItem.create(product, 3);

        product.update(
                "수미감자",
                "이름이 바뀐 상품",
                ProductCategory.AGRICULTURE,
                15000L,
                10,
                "https://cdn.example.com/potato-new.jpg"
        );

        assertThat(orderItem.getProductNameSnapshot()).isEqualTo("감자");
        assertThat(orderItem.getProductPriceSnapshot()).isEqualTo(12000L);
        assertThat(orderItem.getLineAmount()).isEqualTo(36000L);
    }

    private User createSeller(String email, String nickname) {
        User seller = User.createUser(email, "encoded-password", nickname);
        seller.approveSeller(LocalDateTime.of(2026, 3, 11, 10, 0));
        return seller;
    }
}
