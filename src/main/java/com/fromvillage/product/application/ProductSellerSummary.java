package com.fromvillage.product.application;

import com.fromvillage.product.domain.Product;
import com.fromvillage.product.domain.ProductCategory;
import com.fromvillage.product.domain.ProductStatus;

import java.time.LocalDateTime;

public record ProductSellerSummary(
        Long productId,
        String name,
        ProductCategory category,
        Long price,
        Integer stockQuantity,
        ProductStatus status,
        LocalDateTime deletedAt
) {

    public static ProductSellerSummary from(Product product) {
        return new ProductSellerSummary(
                product.getId(),
                product.getName(),
                product.getCategory(),
                product.getPrice(),
                product.getStockQuantity(),
                product.getStatus(),
                product.getDeletedAt()
        );
    }
}
