package com.fromvillage.order.infrastructure;

import com.fromvillage.order.domain.SellerOrder;
import com.fromvillage.order.domain.SellerOrderStore;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Component;

import java.util.List;
import java.util.Optional;

@Component
@RequiredArgsConstructor
public class SellerOrderStoreJpaAdapter implements SellerOrderStore {

    private final SellerOrderJpaRepository sellerOrderJpaRepository;

    @Override
    public Optional<SellerOrder> findById(Long sellerOrderId) {
        return sellerOrderJpaRepository.findByIdWithCheckoutOrderAndItems(sellerOrderId);
    }

    @Override
    public List<SellerOrder> findAllByCheckoutOrderId(Long checkoutOrderId) {
        return sellerOrderJpaRepository.findAllByCheckoutOrderIdWithItems(checkoutOrderId);
    }

    @Override
    public List<SellerOrder> findAllBySellerId(Long sellerId) {
        return sellerOrderJpaRepository.findAllBySellerIdWithItems(sellerId);
    }
}
