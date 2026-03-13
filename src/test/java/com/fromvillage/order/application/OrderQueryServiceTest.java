package com.fromvillage.order.application;

import com.fromvillage.common.exception.BusinessException;
import com.fromvillage.common.exception.ErrorCode;
import com.fromvillage.order.domain.CheckoutOrder;
import com.fromvillage.order.domain.CheckoutOrderQueryPort;
import com.fromvillage.order.domain.OrderItem;
import com.fromvillage.order.domain.SellerOrder;
import com.fromvillage.product.domain.Product;
import com.fromvillage.product.domain.ProductCategory;
import com.fromvillage.user.domain.User;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.test.util.ReflectionTestUtils;

import java.time.LocalDateTime;
import java.util.List;
import java.util.Optional;

import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.anyLong;
import static org.mockito.BDDMockito.given;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;

@ExtendWith(MockitoExtension.class)
class OrderQueryServiceTest {

    @Mock
    private CheckoutOrderQueryPort checkoutOrderQueryPort;

    @Test
    @DisplayName("타인 주문 상세 조회는 상세 그래프를 읽기 전에 거절한다")
    void getOrderRejectsAnotherUsersOrderBeforeLoadingDetail() {
        OrderQueryService orderQueryService = new OrderQueryService(checkoutOrderQueryPort);

        given(checkoutOrderQueryPort.findOwnerIdById(200L)).willReturn(Optional.of(101L));

        assertThatThrownBy(() -> orderQueryService.getOrder(100L, 200L))
                .isInstanceOf(BusinessException.class)
                .extracting("errorCode")
                .isEqualTo(ErrorCode.AUTH_FORBIDDEN);

        verify(checkoutOrderQueryPort, never()).findDetailById(anyLong());
    }

    @Test
    @DisplayName("존재하지 않는 주문 상세 조회는 상세 그래프를 읽기 전에 404를 반환한다")
    void getOrderRejectsMissingOrderBeforeLoadingDetail() {
        OrderQueryService orderQueryService = new OrderQueryService(checkoutOrderQueryPort);

        given(checkoutOrderQueryPort.findOwnerIdById(999L)).willReturn(Optional.empty());

        assertThatThrownBy(() -> orderQueryService.getOrder(100L, 999L))
                .isInstanceOf(BusinessException.class)
                .extracting("errorCode")
                .isEqualTo(ErrorCode.ORDER_NOT_FOUND);

        verify(checkoutOrderQueryPort, never()).findDetailById(anyLong());
    }

    @Test
    @DisplayName("본인 주문 상세 조회는 소유권 확인 후 상세 그래프를 읽는다")
    void getOrderLoadsDetailAfterOwnershipCheck() {
        OrderQueryService orderQueryService = new OrderQueryService(checkoutOrderQueryPort);

        User buyer = user(100L, "buyer@example.com", "구매자");
        User seller = seller(1L, "seller@example.com", "판매자");
        Product potato = product(seller, "감자", 12000L, 3);
        CheckoutOrder checkoutOrder = completedOrder(
                200L,
                buyer,
                List.of(
                        SellerOrder.create(seller, List.of(OrderItem.create(potato, 1)))
                )
        );

        given(checkoutOrderQueryPort.findOwnerIdById(200L)).willReturn(Optional.of(100L));
        given(checkoutOrderQueryPort.findDetailById(200L)).willReturn(Optional.of(checkoutOrder));

        orderQueryService.getOrder(100L, 200L);

        verify(checkoutOrderQueryPort).findOwnerIdById(200L);
        verify(checkoutOrderQueryPort).findDetailById(200L);
    }

    private CheckoutOrder completedOrder(Long orderId, User buyer, List<SellerOrder> sellerOrders) {
        CheckoutOrder checkoutOrder = CheckoutOrder.create(buyer, sellerOrders);
        ReflectionTestUtils.setField(checkoutOrder, "id", orderId);
        checkoutOrder.complete(LocalDateTime.of(2026, 3, 13, 10, 0));
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
