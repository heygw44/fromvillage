package com.fromvillage.product.presentation;

import com.fromvillage.product.application.ProductManageResult;

import java.time.LocalDateTime;

public record ProductManageResponse(
        Long productId,
        String name,
        String description,
        String category,
        Long price,
        Integer stockQuantity,
        String status,
        String imageUrl,
        LocalDateTime deletedAt
) {

    public static ProductManageResponse from(ProductManageResult result) {
        return new ProductManageResponse(
                result.productId(),
                result.name(),
                result.description(),
                result.category().name(),
                result.price(),
                result.stockQuantity(),
                result.status().name(),
                result.imageUrl(),
                result.deletedAt()
        );
    }
}
