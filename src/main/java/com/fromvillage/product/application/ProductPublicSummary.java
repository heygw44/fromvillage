package com.fromvillage.product.application;

import com.fromvillage.product.domain.Product;

public record ProductPublicSummary(
        Long productId,
        String name,
        String category,
        Long price,
        Integer stockQuantity,
        String status,
        String imageUrl
) {

    public static ProductPublicSummary from(Product product) {
        return new ProductPublicSummary(
                product.getId(),
                product.getName(),
                product.getCategory().name(),
                product.getPrice(),
                product.getStockQuantity(),
                product.getStatus().name(),
                product.getImageUrl()
        );
    }
}
