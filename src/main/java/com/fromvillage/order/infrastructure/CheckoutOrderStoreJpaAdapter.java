package com.fromvillage.order.infrastructure;

import com.fromvillage.order.domain.CheckoutOrder;
import com.fromvillage.order.domain.CheckoutOrderStore;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Component;

import java.util.Optional;

@Component
@RequiredArgsConstructor
public class CheckoutOrderStoreJpaAdapter implements CheckoutOrderStore {

    private final CheckoutOrderJpaRepository checkoutOrderJpaRepository;
    private final SellerOrderJpaRepository sellerOrderJpaRepository;

    @Override
    public CheckoutOrder save(CheckoutOrder checkoutOrder) {
        return checkoutOrderJpaRepository.saveAndFlush(checkoutOrder);
    }

    @Override
    public Optional<CheckoutOrder> findById(Long checkoutOrderId) {
        Optional<CheckoutOrder> checkoutOrder = checkoutOrderJpaRepository.findByIdWithSellerOrders(checkoutOrderId);
        checkoutOrder.ifPresent(order -> sellerOrderJpaRepository.findAllByCheckoutOrderIdWithItems(order.getId()));
        return checkoutOrder;
    }
}
