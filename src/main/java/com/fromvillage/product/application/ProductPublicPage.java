package com.fromvillage.product.application;

import com.fromvillage.product.domain.Product;
import com.fromvillage.product.domain.ProductPageResult;

import java.util.List;

public record ProductPublicPage(
        List<ProductPublicSummary> content,
        int page,
        int size,
        long totalElements,
        int totalPages,
        boolean hasNext
) {

    public static ProductPublicPage from(ProductPageResult<Product> page) {
        return new ProductPublicPage(
                page.content().stream()
                        .map(ProductPublicSummary::from)
                        .toList(),
                page.page(),
                page.size(),
                page.totalElements(),
                page.totalPages(),
                page.hasNext()
        );
    }
}
