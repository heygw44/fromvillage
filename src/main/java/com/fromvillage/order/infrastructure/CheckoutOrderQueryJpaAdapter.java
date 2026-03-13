package com.fromvillage.order.infrastructure;

import com.fromvillage.order.domain.CheckoutOrder;
import com.fromvillage.order.domain.CheckoutOrderQueryPort;
import lombok.RequiredArgsConstructor;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.stereotype.Component;
import org.springframework.transaction.annotation.Transactional;

import java.util.Optional;

@Component
@RequiredArgsConstructor
public class CheckoutOrderQueryJpaAdapter implements CheckoutOrderQueryPort {

    private final CheckoutOrderJpaRepository checkoutOrderJpaRepository;
    private final SellerOrderJpaRepository sellerOrderJpaRepository;

    @Override
    public Page<CheckoutOrder> findAllByUserId(Long userId, Pageable pageable) {
        return checkoutOrderJpaRepository.findAllByUserId(userId, pageable);
    }

    @Override
    @Transactional(readOnly = true)
    public Optional<CheckoutOrder> findById(Long orderId) {
        Optional<CheckoutOrder> checkoutOrder = checkoutOrderJpaRepository.findByIdWithSellerOrders(orderId);
        checkoutOrder.ifPresent(order -> sellerOrderJpaRepository.findAllByCheckoutOrderIdWithItems(order.getId()));
        return checkoutOrder;
    }
}
