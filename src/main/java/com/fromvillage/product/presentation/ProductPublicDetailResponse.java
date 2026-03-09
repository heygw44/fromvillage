package com.fromvillage.product.presentation;

import com.fromvillage.product.application.ProductPublicDetail;

public record ProductPublicDetailResponse(
        Long productId,
        String name,
        String description,
        String category,
        Long price,
        Integer stockQuantity,
        String status,
        String imageUrl
) {

    public static ProductPublicDetailResponse from(ProductPublicDetail detail) {
        return new ProductPublicDetailResponse(
                detail.productId(),
                detail.name(),
                detail.description(),
                detail.category(),
                detail.price(),
                detail.stockQuantity(),
                detail.status(),
                detail.imageUrl()
        );
    }
}
