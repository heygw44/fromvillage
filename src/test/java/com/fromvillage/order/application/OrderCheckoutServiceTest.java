package com.fromvillage.order.application;

import com.fromvillage.cart.domain.CartItem;
import com.fromvillage.cart.domain.CartStore;
import com.fromvillage.common.exception.BusinessException;
import com.fromvillage.common.exception.ErrorCode;
import com.fromvillage.order.domain.CheckoutOrder;
import com.fromvillage.product.domain.Product;
import com.fromvillage.product.domain.ProductCategory;
import com.fromvillage.user.domain.User;
import com.fromvillage.user.domain.UserStore;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.test.util.ReflectionTestUtils;

import java.time.LocalDateTime;
import java.util.List;
import java.util.Optional;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.ArgumentMatchers.anyList;
import static org.mockito.BDDMockito.given;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;

@ExtendWith(MockitoExtension.class)
class OrderCheckoutServiceTest {

    @Mock
    private UserStore userStore;

    @Mock
    private CartStore cartStore;

    @Mock
    private OrderPlacementService orderPlacementService;

    private OrderCheckoutService orderCheckoutService;

    @BeforeEach
    void setUp() {
        orderCheckoutService = new OrderCheckoutService(userStore, cartStore, orderPlacementService);
    }

    @Test
    @DisplayName("체크아웃이 성공하면 주문 생성 후 해당 장바구니 항목을 삭제한다")
    void checkoutPlacesOrderAndDeletesCartItems() {
        User buyer = user(100L, "buyer@example.com", "구매자");
        User seller = seller(1L, "seller@example.com", "판매자");

        Product potato = Product.create(
                seller,
                "유기농 감자 5kg",
                "해남 햇감자",
                ProductCategory.AGRICULTURE,
                22000L,
                10,
                "https://cdn.example.com/potato.jpg"
        );
        Product cabbage = Product.create(
                seller,
                "절임배추 10kg",
                "전남 해남 절임배추",
                ProductCategory.AGRICULTURE,
                18000L,
                8,
                "https://cdn.example.com/cabbage.jpg"
        );

        List<CartItem> cartItems = List.of(
                CartItem.create(buyer, potato, 2),
                CartItem.create(buyer, cabbage, 1)
        );
        CheckoutOrder checkoutOrder = CheckoutOrder.create(
                buyer,
                List.of(
                        com.fromvillage.order.domain.SellerOrder.create(
                                seller,
                                List.of(
                                        com.fromvillage.order.domain.OrderItem.create(potato, 2),
                                        com.fromvillage.order.domain.OrderItem.create(cabbage, 1)
                                )
                        )
                )
        );
        checkoutOrder.complete(LocalDateTime.of(2026, 3, 12, 12, 0));

        given(userStore.findById(100L)).willReturn(Optional.of(buyer));
        given(cartStore.findAllForCheckoutByUserId(100L)).willReturn(cartItems);
        given(orderPlacementService.place(buyer, List.of(
                OrderCheckoutLine.fromCartItem(cartItems.get(0)),
                OrderCheckoutLine.fromCartItem(cartItems.get(1))
        ))).willReturn(checkoutOrder);

        OrderCheckoutResult result = orderCheckoutService.checkout(100L);

        assertThat(result.sellerOrderCount()).isEqualTo(1);
        assertThat(result.totalAmount()).isEqualTo(62000L);
        assertThat(result.finalAmount()).isEqualTo(62000L);
        verify(cartStore).deleteAll(cartItems);
    }

    @Test
    @DisplayName("빈 장바구니면 주문 생성 로직을 호출하지 않고 예외를 던진다")
    void checkoutRejectsEmptyCartBeforeOrderPlacement() {
        User buyer = user(100L, "buyer@example.com", "구매자");

        given(userStore.findById(100L)).willReturn(Optional.of(buyer));
        given(cartStore.findAllForCheckoutByUserId(100L)).willReturn(List.of());

        assertThatThrownBy(() -> orderCheckoutService.checkout(100L))
                .isInstanceOf(BusinessException.class)
                .extracting("errorCode")
                .isEqualTo(ErrorCode.ORDER_CHECKOUT_CART_EMPTY);

        verify(orderPlacementService, never()).place(eq(buyer), anyList());
        verify(cartStore, never()).deleteAll(anyList());
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
