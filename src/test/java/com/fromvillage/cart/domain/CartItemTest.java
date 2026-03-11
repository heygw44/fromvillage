package com.fromvillage.cart.domain;

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

class CartItemTest {

    @Test
    @DisplayName("장바구니 항목을 생성할 수 있다")
    void createCartItem() {
        User cartUser = User.createUser("user@example.com", "encoded-password", "buyer");
        User seller = createSeller("seller@example.com", "seller");
        Product product = Product.create(
                seller,
                "감자",
                "해남 햇감자",
                ProductCategory.AGRICULTURE,
                12000L,
                5,
                "https://cdn.example.com/potato.jpg");

        CartItem cartItem = CartItem.create(cartUser, product, 2);

        assertThat(cartItem.getUser()).isEqualTo(cartUser);
        assertThat(cartItem.getProduct()).isEqualTo(product);
        assertThat(cartItem.getQuantity()).isEqualTo(2);
    }

    @Test
    @DisplayName("장바구니 수량은 1 이상이어야 한다")
    void createCartItemRejectsInvalidQuantity() {
        User cartUser = User.createUser("user@example.com", "encoded-password", "buyer");
        User seller = createSeller("seller@example.com", "seller");
        Product product = Product.create(
                seller,
                "감자",
                "해남 햇감자",
                ProductCategory.AGRICULTURE,
                12000L,
                5,
                "https://cdn.example.com/potato.jpg");

        assertThatThrownBy(() -> CartItem.create(cartUser, product, 0))
                .isInstanceOf(BusinessException.class)
                .extracting(exception -> ((BusinessException) exception).getErrorCode())
                .isEqualTo(ErrorCode.CART_QUANTITY_INVALID);
    }

    @Test
    @DisplayName("장바구니 수량을 변경할 수 있다")
    void changeQuantity() {
        User cartUser = User.createUser("user@example.com", "encoded-password", "buyer");
        User seller = createSeller("seller@example.com", "seller");
        Product product = Product.create(
                seller,
                "감자",
                "해남 햇감자",
                ProductCategory.AGRICULTURE,
                12000L,
                5,
                "https://cdn.example.com/potato.jpg");
        CartItem cartItem = CartItem.create(cartUser, product, 1);

        cartItem.changeQuantity(3);

        assertThat(cartItem.getQuantity()).isEqualTo(3);
    }

    @Test
    @DisplayName("장바구니 수량 변경 시 1 미만은 허용하지 않는다")
    void changeQuantityRejectsInvalidQuantity() {
        User cartUser = User.createUser("user@example.com", "encoded-password", "buyer");
        User seller = createSeller("seller@example.com", "seller");
        Product product = Product.create(
                seller,
                "감자",
                "해남 햇감자",
                ProductCategory.AGRICULTURE,
                12000L,
                5,
                "https://cdn.example.com/potato.jpg");
        CartItem cartItem = CartItem.create(cartUser, product, 1);

        assertThatThrownBy(() -> cartItem.changeQuantity(0))
                .isInstanceOf(BusinessException.class)
                .extracting(exception -> ((BusinessException) exception).getErrorCode())
                .isEqualTo(ErrorCode.CART_QUANTITY_INVALID);
    }

    private User createSeller(String email, String nickname) {
        User seller = User.createUser(email, "encoded-password", nickname);
        seller.approveSeller(LocalDateTime.of(2026, 3, 11, 10, 0));
        return seller;
    }
}
