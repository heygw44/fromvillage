package com.fromvillage.product.application;

import com.fromvillage.product.domain.Product;
import org.springframework.data.domain.Page;

import java.util.List;

public record ProductPublicPage(
        List<ProductPublicSummary> content,
        int page,
        int size,
        long totalElements,
        int totalPages,
        boolean hasNext
) {

    public static ProductPublicPage from(Page<Product> page) {
        return new ProductPublicPage(
                page.getContent().stream()
                        .map(ProductPublicSummary::from)
                        .toList(),
                page.getNumber(),
                page.getSize(),
                page.getTotalElements(),
                page.getTotalPages(),
                page.hasNext()
        );
    }
}
