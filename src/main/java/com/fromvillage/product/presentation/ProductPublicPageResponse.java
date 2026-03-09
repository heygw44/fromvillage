package com.fromvillage.product.presentation;

import com.fromvillage.product.application.ProductPublicPage;

import java.util.List;

public record ProductPublicPageResponse(
        List<ProductPublicSummaryResponse> content,
        int page,
        int size,
        long totalElements,
        int totalPages,
        boolean hasNext
) {

    public static ProductPublicPageResponse from(ProductPublicPage page) {
        return new ProductPublicPageResponse(
                page.content().stream()
                        .map(ProductPublicSummaryResponse::from)
                        .toList(),
                page.page(),
                page.size(),
                page.totalElements(),
                page.totalPages(),
                page.hasNext()
        );
    }
}
