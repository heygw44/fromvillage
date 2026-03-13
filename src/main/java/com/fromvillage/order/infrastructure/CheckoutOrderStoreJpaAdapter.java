package com.fromvillage.order.infrastructure;

import com.fromvillage.order.domain.CheckoutOrder;
import com.fromvillage.order.domain.CheckoutOrderStore;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Component;

@Component
@RequiredArgsConstructor
public class CheckoutOrderStoreJpaAdapter implements CheckoutOrderStore {

    private final CheckoutOrderJpaRepository checkoutOrderJpaRepository;

    @Override
    public CheckoutOrder save(CheckoutOrder checkoutOrder) {
        return checkoutOrderJpaRepository.saveAndFlush(checkoutOrder);
    }
}
