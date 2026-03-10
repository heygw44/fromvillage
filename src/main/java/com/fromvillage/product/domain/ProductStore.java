package com.fromvillage.product.domain;

import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;

import java.util.Optional;

public interface ProductStore {

    Product save(Product product);

    Optional<Product> findById(Long productId);

    Page<Product> findAllBySellerIdIncludingDeleted(Long sellerId, Pageable pageable);
}
