package com.fromvillage.product.presentation;

import com.fromvillage.product.application.ProductSellerPage;
import com.fromvillage.product.application.ProductSellerSummary;

import java.time.LocalDateTime;
import java.util.List;

public record ProductSellerPageResponse(
        List<ProductSellerSummaryResponse> content,
        int page,
        int size,
        long totalElements,
        int totalPages,
        boolean hasNext
) {

    public static ProductSellerPageResponse from(ProductSellerPage page) {
        return new ProductSellerPageResponse(
                page.content().stream()
                        .map(ProductSellerSummaryResponse::from)
                        .toList(),
                page.page(),
                page.size(),
                page.totalElements(),
                page.totalPages(),
                page.hasNext()
        );
    }

    public record ProductSellerSummaryResponse(
            Long productId,
            String name,
            String category,
            Long price,
            Integer stockQuantity,
            String status,
            LocalDateTime deletedAt
    ) {
        public static ProductSellerSummaryResponse from(ProductSellerSummary summary) {
            return new ProductSellerSummaryResponse(
                    summary.productId(),
                    summary.name(),
                    summary.category().name(),
                    summary.price(),
                    summary.stockQuantity(),
                    summary.status().name(),
                    summary.deletedAt()
            );
        }
    }
}
