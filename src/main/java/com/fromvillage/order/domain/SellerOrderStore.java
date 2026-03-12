package com.fromvillage.order.domain;

import java.util.List;
import java.util.Optional;

public interface SellerOrderStore {

    Optional<SellerOrder> findById(Long sellerOrderId);

    List<SellerOrder> findAllByCheckoutOrderId(Long checkoutOrderId);

    List<SellerOrder> findAllBySellerId(Long sellerId);
}
