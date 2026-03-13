package com.fromvillage.order.domain;

import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;

import java.util.Optional;

public interface CheckoutOrderQueryPort {

    Page<CheckoutOrder> findAllByUserId(Long userId, Pageable pageable);

    Optional<CheckoutOrder> findById(Long orderId);
}
