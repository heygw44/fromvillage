package com.fromvillage.order.application;

import com.fromvillage.common.exception.BusinessException;
import com.fromvillage.common.exception.ErrorCode;
import com.fromvillage.product.domain.Product;
import com.fromvillage.product.domain.ProductCategory;
import com.fromvillage.user.domain.User;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.test.util.ReflectionTestUtils;

import java.time.LocalDateTime;

import static org.assertj.core.api.Assertions.assertThatThrownBy;

class OrderCheckoutLineTest {

    @Test
    @DisplayName("주문 라인 수량은 1개 이상이어야 한다")
    void orderCheckoutLineRequiresPositiveQuantity() {
        User seller = seller(1L, "seller@example.com", "판매자");
        Product product = Product.create(
                seller,
                "유기농 감자 5kg",
                "해남 햇감자",
                ProductCategory.AGRICULTURE,
                22000L,
                10,
                "https://cdn.example.com/potato.jpg"
        );

        assertThatThrownBy(() -> OrderCheckoutLine.of(product, 0))
                .isInstanceOf(BusinessException.class)
                .extracting("errorCode")
                .isEqualTo(ErrorCode.ORDER_ITEM_QUANTITY_INVALID);
    }

    private User seller(Long id, String email, String nickname) {
        User seller = User.createUser(email, "encoded-password", nickname);
        seller.approveSeller(LocalDateTime.of(2026, 3, 12, 12, 0));
        ReflectionTestUtils.setField(seller, "id", id);
        return seller;
    }
}
