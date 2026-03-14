package com.fromvillage.order.domain;

import java.util.Optional;

public interface SellerOrderQueryPort {

    OrderPageResult<SellerOrderSummaryView> findSellerOrderSummariesBySellerId(Long sellerId, OrderPageRequest pageRequest);

    Optional<Long> findSellerIdBySellerOrderId(Long sellerOrderId);

    Optional<SellerOrder> findDetailBySellerOrderId(Long sellerOrderId);
}
