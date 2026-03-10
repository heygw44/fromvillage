package com.fromvillage.product.infrastructure;

import com.fromvillage.product.domain.Product;
import com.fromvillage.product.domain.ProductStore;
import lombok.RequiredArgsConstructor;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.stereotype.Component;

import java.util.Optional;

@Component
@RequiredArgsConstructor
public class ProductStoreJpaAdapter implements ProductStore {

    private final ProductJpaRepository productJpaRepository;

    @Override
    public Product save(Product product) {
        return productJpaRepository.saveAndFlush(product);
    }

    @Override
    public Optional<Product> findById(Long productId) {
        return productJpaRepository.findById(productId);
    }

    @Override
    public Page<Product> findAllBySellerIdIncludingDeleted(Long sellerId, Pageable pageable) {
        return productJpaRepository.findSellerProductsIncludingDeleted(sellerId, pageable);
    }
}
