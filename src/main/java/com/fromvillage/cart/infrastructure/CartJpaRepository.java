package com.fromvillage.cart.infrastructure;

import com.fromvillage.cart.domain.CartItem;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;

import java.util.List;
import java.util.Optional;

public interface CartJpaRepository extends JpaRepository<CartItem, Long> {

    Optional<CartItem> findByUserIdAndProductId(Long userId, Long productId);

    List<CartItem> findAllByUserId(Long userId);

    @Query("""
            select cartItem
            from CartItem cartItem
            join fetch cartItem.product product
            where cartItem.user.id = :userId
              and product.deletedAt is null
            """)
    List<CartItem> findAllActiveByUserId(Long userId);
}
