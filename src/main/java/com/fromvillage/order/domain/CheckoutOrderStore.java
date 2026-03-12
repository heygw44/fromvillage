package com.fromvillage.order.domain;

import java.util.Optional;

public interface CheckoutOrderStore {

    CheckoutOrder save(CheckoutOrder checkoutOrder);

    Optional<CheckoutOrder> findById(Long checkoutOrderId);
}
