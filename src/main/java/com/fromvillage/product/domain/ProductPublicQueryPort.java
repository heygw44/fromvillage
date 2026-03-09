package com.fromvillage.product.domain;

import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;

import java.util.Optional;

public interface ProductPublicQueryPort {

    Page<Product> findPublicProducts(String keyword, ProductCategory category, Pageable pageable);

    Optional<Product> findPublicProductById(Long productId);
}
