package com.fromvillage.product.presentation;

import com.fromvillage.product.application.ProductPublicSummary;

public record ProductPublicSummaryResponse(
        Long productId,
        String name,
        String category,
        Long price,
        Integer stockQuantity,
        String status,
        String imageUrl
) {

    public static ProductPublicSummaryResponse from(ProductPublicSummary summary) {
        return new ProductPublicSummaryResponse(
                summary.productId(),
                summary.name(),
                summary.category(),
                summary.price(),
                summary.stockQuantity(),
                summary.status(),
                summary.imageUrl()
        );
    }
}
