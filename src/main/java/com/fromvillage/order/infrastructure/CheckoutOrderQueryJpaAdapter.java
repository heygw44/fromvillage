package com.fromvillage.order.infrastructure;

import com.fromvillage.order.domain.CheckoutOrder;
import com.fromvillage.order.domain.CheckoutOrderQueryPort;
import com.fromvillage.order.domain.CheckoutOrderSummaryView;
import com.fromvillage.order.domain.OrderPageRequest;
import com.fromvillage.order.domain.OrderPageResult;
import com.fromvillage.order.domain.OrderQuerySort;
import lombok.RequiredArgsConstructor;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Pageable;
import org.springframework.data.domain.Sort;
import org.springframework.stereotype.Component;

import java.util.Optional;

@Component
@RequiredArgsConstructor
public class CheckoutOrderQueryJpaAdapter implements CheckoutOrderQueryPort {

    private final CheckoutOrderJpaRepository checkoutOrderJpaRepository;
    private final SellerOrderJpaRepository sellerOrderJpaRepository;

    @Override
    public OrderPageResult<CheckoutOrderSummaryView> findOrderSummariesByUserId(Long userId, OrderPageRequest pageRequest) {
        Page<CheckoutOrderSummaryView> page = checkoutOrderJpaRepository.findOrderSummariesByUserId(
                userId,
                toPageable(pageRequest)
        );

        return new OrderPageResult<>(
                page.getContent(),
                page.getNumber(),
                page.getSize(),
                page.getTotalElements(),
                page.getTotalPages(),
                page.hasNext()
        );
    }

    @Override
    public Optional<Long> findOwnerIdByOrderNumber(String orderNumber) {
        return checkoutOrderJpaRepository.findOwnerIdByOrderNumber(orderNumber);
    }

    @Override
    public Optional<CheckoutOrder> findDetailByOrderNumber(String orderNumber) {
        return checkoutOrderJpaRepository.findByOrderNumberWithSellerOrders(orderNumber)
                .map(this::loadOrderItemsInPersistenceContext);
    }

    private CheckoutOrder loadOrderItemsInPersistenceContext(CheckoutOrder checkoutOrder) {
        // 한 번에 두 개의 목록 컬렉션을 함께 당겨 오지 않고, 같은 영속성 컨텍스트에서 하위 주문 상품까지 채워 둔다.
        sellerOrderJpaRepository.findAllByCheckoutOrderIdWithItems(checkoutOrder.getId());
        return checkoutOrder;
    }

    private Pageable toPageable(OrderPageRequest pageRequest) {
        return PageRequest.of(pageRequest.page(), pageRequest.size(), toSort(pageRequest.sort()));
    }

    private Sort toSort(OrderQuerySort sort) {
        return switch (sort) {
            case CREATED_AT_DESC -> Sort.by(
                    Sort.Order.desc("createdAt"),
                    Sort.Order.desc("id")
            );
            case CREATED_AT_ASC -> Sort.by(
                    Sort.Order.asc("createdAt"),
                    Sort.Order.asc("id")
            );
        };
    }
}
