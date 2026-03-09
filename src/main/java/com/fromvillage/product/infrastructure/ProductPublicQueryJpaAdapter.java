package com.fromvillage.product.infrastructure;

import com.fromvillage.product.domain.Product;
import com.fromvillage.product.domain.ProductCategory;
import com.fromvillage.product.domain.ProductPublicQueryPort;
import lombok.RequiredArgsConstructor;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.stereotype.Component;

import java.util.Optional;

@Component
@RequiredArgsConstructor
public class ProductPublicQueryJpaAdapter implements ProductPublicQueryPort {

    private final ProductJpaRepository productJpaRepository;

    @Override
    public Page<Product> findPublicProducts(String keyword, ProductCategory category, Pageable pageable) {
        return productJpaRepository.findPublicProducts(keyword, category, pageable);
    }

    @Override
    public Optional<Product> findPublicProductById(Long productId) {
        return productJpaRepository.findByIdAndDeletedAtIsNull(productId);
    }
}
