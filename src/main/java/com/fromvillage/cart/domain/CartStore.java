package com.fromvillage.cart.domain;

import java.util.List;
import java.util.Optional;

public interface CartStore {

    CartItem save(CartItem cartItem);

    void delete(CartItem cartItem);

    void deleteAll(Iterable<CartItem> cartItems);

    Optional<CartItem> findById(Long cartItemId);

    Optional<CartItem> findByUserIdAndProductId(Long userId, Long productId);

    List<CartItem> findAllByUserId(Long userId);

    List<CartItem> findAllActiveByUserId(Long userId);

    List<CartItem> findAllForCheckoutByUserId(Long userId);
}
