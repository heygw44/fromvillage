package com.fromvillage.product.application;

import com.fromvillage.product.domain.Product;
import com.fromvillage.product.domain.ProductCategory;
import com.fromvillage.product.domain.ProductStatus;

import java.time.LocalDateTime;

public record ProductManageResult(
        Long productId,
        String name,
        String description,
        ProductCategory category,
        Long price,
        Integer stockQuantity,
        ProductStatus status,
        String imageUrl,
        LocalDateTime deletedAt
) {

    public static ProductManageResult from(Product product) {
        return new ProductManageResult(
                product.getId(),
                product.getName(),
                product.getDescription(),
                product.getCategory(),
                product.getPrice(),
                product.getStockQuantity(),
                product.getStatus(),
                product.getImageUrl(),
                product.getDeletedAt()
        );
    }
}
