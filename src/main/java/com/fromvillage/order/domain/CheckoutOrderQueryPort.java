package com.fromvillage.order.domain;

import java.util.Optional;

public interface CheckoutOrderQueryPort {

    OrderPageResult<CheckoutOrderSummaryView> findOrderSummariesByUserId(Long userId, OrderPageRequest pageRequest);

    Optional<CheckoutOrder> findDetailById(Long orderId);
}
