package com.fromvillage.product.infrastructure;

import com.fromvillage.product.domain.Product;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;

public interface ProductJpaRepository extends JpaRepository<Product, Long> {

    Page<Product> findAllBySellerId(Long sellerId, Pageable pageable);
}
