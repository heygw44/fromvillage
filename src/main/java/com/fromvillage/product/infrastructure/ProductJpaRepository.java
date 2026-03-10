package com.fromvillage.product.infrastructure;

import com.fromvillage.product.domain.Product;
import com.fromvillage.product.domain.ProductCategory;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import java.util.Optional;

public interface ProductJpaRepository extends JpaRepository<Product, Long> {

    @Query("""
            select p
            from Product p
            where p.seller.id = :sellerId
            """)
    Page<Product> findSellerProductsIncludingDeleted(
            @Param("sellerId") Long sellerId,
            Pageable pageable
    );

    @Query("""
            select p
            from Product p
            where p.deletedAt is null
              and (:keyword is null or lower(p.name) like lower(concat('%', :keyword, '%')))
              and (:category is null or p.category = :category)
            """)
    Page<Product> findPublicProducts(
            @Param("keyword") String keyword,
            @Param("category") ProductCategory category,
            Pageable pageable
    );

    Optional<Product> findByIdAndDeletedAtIsNull(Long productId);
}
