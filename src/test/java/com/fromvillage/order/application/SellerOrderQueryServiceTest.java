package com.fromvillage.order.application;

import com.fromvillage.common.exception.BusinessException;
import com.fromvillage.common.exception.ErrorCode;
import com.fromvillage.order.domain.OrderItem;
import com.fromvillage.order.domain.SellerOrder;
import com.fromvillage.order.domain.SellerOrderQueryPort;
import com.fromvillage.user.domain.User;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Sort;

import java.time.LocalDateTime;
import java.util.List;
import java.util.Optional;

import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.anyLong;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.BDDMockito.given;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;

class SellerOrderQueryServiceTest {

    private final SellerOrderQueryPort sellerOrderQueryPort = mock(SellerOrderQueryPort.class);
    private final SellerOrderQueryService sellerOrderQueryService = new SellerOrderQueryService(sellerOrderQueryPort);

    @Test
    @DisplayName("다른 SELLER 주문 상세 조회는 상세 그래프를 읽기 전에 거절한다")
    void getSellerOrderRejectsAnotherSellersOrderBeforeLoadingDetail() {
        given(sellerOrderQueryPort.findSellerIdBySellerOrderId(200L)).willReturn(Optional.of(101L));

        assertThatThrownBy(() -> sellerOrderQueryService.getSellerOrder(100L, 200L))
                .isInstanceOf(BusinessException.class)
                .extracting("errorCode")
                .isEqualTo(ErrorCode.AUTH_FORBIDDEN);

        verify(sellerOrderQueryPort, never()).findDetailBySellerOrderId(anyLong());
    }

    @Test
    @DisplayName("존재하지 않는 SELLER 주문 상세 조회는 상세 그래프를 읽기 전에 404를 반환한다")
    void getSellerOrderRejectsMissingOrderBeforeLoadingDetail() {
        given(sellerOrderQueryPort.findSellerIdBySellerOrderId(999L)).willReturn(Optional.empty());

        assertThatThrownBy(() -> sellerOrderQueryService.getSellerOrder(100L, 999L))
                .isInstanceOf(BusinessException.class)
                .extracting("errorCode")
                .isEqualTo(ErrorCode.ORDER_NOT_FOUND);

        verify(sellerOrderQueryPort, never()).findDetailBySellerOrderId(anyLong());
    }

    @Test
    @DisplayName("SELLER 주문 목록 조회는 createdAt 외의 정렬 키를 허용하지 않는다")
    void getSellerOrdersRejectsUnsupportedSort() {
        assertThatThrownBy(() -> sellerOrderQueryService.getSellerOrders(
                100L,
                PageRequest.of(0, 20, Sort.by(Sort.Direction.ASC, "status"))
        ))
                .isInstanceOf(BusinessException.class)
                .extracting("errorCode")
                .isEqualTo(ErrorCode.VALIDATION_ERROR);
    }

    @Test
    @DisplayName("본인 SELLER 주문 상세 조회는 소유권 확인 후 상세 그래프를 읽는다")
    void getSellerOrderLoadsDetailAfterOwnershipCheck() {
        SellerOrder sellerOrder = completedSellerOrder();

        given(sellerOrderQueryPort.findSellerIdBySellerOrderId(200L)).willReturn(Optional.of(100L));
        given(sellerOrderQueryPort.findDetailBySellerOrderId(200L)).willReturn(Optional.of(sellerOrder));

        sellerOrderQueryService.getSellerOrder(100L, 200L);

        verify(sellerOrderQueryPort).findSellerIdBySellerOrderId(200L);
        verify(sellerOrderQueryPort).findDetailBySellerOrderId(200L);
    }

    private SellerOrder completedSellerOrder() {
        User seller = User.createUser("seller@example.com", "encoded", "판매자");
        User buyer = User.createUser("buyer@example.com", "encoded", "구매자");
        seller.approveSeller(LocalDateTime.of(2026, 3, 13, 10, 0));

        var product = com.fromvillage.product.domain.Product.create(
                seller,
                "감자",
                "감자 설명",
                com.fromvillage.product.domain.ProductCategory.AGRICULTURE,
                12000L,
                10,
                "https://cdn.example.com/potato.jpg"
        );
        var checkoutOrder = com.fromvillage.order.domain.CheckoutOrder.create(
                buyer,
                List.of(SellerOrder.create(seller, List.of(OrderItem.create(product, 1))))
        );
        checkoutOrder.complete(LocalDateTime.of(2026, 3, 14, 10, 0));
        return checkoutOrder.getSellerOrders().getFirst();
    }
}
