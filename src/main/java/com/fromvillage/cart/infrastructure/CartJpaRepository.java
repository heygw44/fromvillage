package com.fromvillage.cart.infrastructure;

import com.fromvillage.cart.domain.CartItem;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;

import java.util.List;
import java.util.Optional;

public interface CartJpaRepository extends JpaRepository<CartItem, Long> {

    @Query("""
            select cartItem
            from CartItem cartItem
            join fetch cartItem.product product
            join fetch product.seller seller
            where cartItem.id = :cartItemId
            """)
    Optional<CartItem> findByIdWithProductAndSeller(Long cartItemId);

    @Query("""
            select cartItem
            from CartItem cartItem
            join fetch cartItem.product product
            join fetch product.seller seller
            where cartItem.user.id = :userId
              and product.id = :productId
            """)
    Optional<CartItem> findByUserIdAndProductIdWithProductAndSeller(Long userId, Long productId);

    @Query("""
            select cartItem
            from CartItem cartItem
            join fetch cartItem.product product
            where cartItem.user.id = :userId
            """)
    List<CartItem> findAllByUserId(Long userId);

    // Keep cart listing aligned with checkout eligibility: only orderable items are exposed here.
    @Query("""
            select cartItem
            from CartItem cartItem
            join fetch cartItem.product product
            join fetch product.seller seller
            where cartItem.user.id = :userId
              and product.deletedAt is null
              and product.status = com.fromvillage.product.domain.ProductStatus.ON_SALE
            """)
    List<CartItem> findAllActiveByUserId(Long userId);
}
