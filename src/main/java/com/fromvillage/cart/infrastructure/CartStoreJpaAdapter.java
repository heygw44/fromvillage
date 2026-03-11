package com.fromvillage.cart.infrastructure;

import com.fromvillage.cart.domain.CartItem;
import com.fromvillage.cart.domain.CartStore;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Component;

import java.util.List;
import java.util.Optional;

@Component
@RequiredArgsConstructor
public class CartStoreJpaAdapter implements CartStore {

    private final CartJpaRepository cartJpaRepository;

    @Override
    public CartItem save(CartItem cartItem) {
        return cartJpaRepository.save(cartItem);
    }

    @Override
    public void delete(CartItem cartItem) {
        cartJpaRepository.delete(cartItem);
    }

    @Override
    public Optional<CartItem> findById(Long cartItemId) {
        return cartJpaRepository.findByIdWithProductAndSeller(cartItemId);
    }

    @Override
    public Optional<CartItem> findByUserIdAndProductId(Long userId, Long productId) {
        return cartJpaRepository.findByUserIdAndProductIdWithProductAndSeller(userId, productId);
    }

    @Override
    public List<CartItem> findAllByUserId(Long userId) {
        return cartJpaRepository.findAllByUserId(userId);
    }

    @Override
    public List<CartItem> findAllActiveByUserId(Long userId) {
        return cartJpaRepository.findAllActiveByUserId(userId);
    }
}
