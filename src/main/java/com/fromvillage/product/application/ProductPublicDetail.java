package com.fromvillage.product.application;

import com.fromvillage.product.domain.Product;

public record ProductPublicDetail(
        Long productId,
        String name,
        String description,
        String category,
        Long price,
        Integer stockQuantity,
        String status,
        String imageUrl
) {

    public static ProductPublicDetail from(Product product) {
        return new ProductPublicDetail(
                product.getId(),
                product.getName(),
                product.getDescription(),
                product.getCategory().name(),
                product.getPrice(),
                product.getStockQuantity(),
                product.getStatus().name(),
                product.getImageUrl()
        );
    }
}
