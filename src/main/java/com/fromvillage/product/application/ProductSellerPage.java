package com.fromvillage.product.application;

import org.springframework.data.domain.Page;

import java.util.List;

public record ProductSellerPage(
        List<ProductSellerSummary> content,
        int page,
        int size,
        long totalElements,
        int totalPages,
        boolean hasNext
) {

    public static ProductSellerPage from(Page<ProductSellerSummary> page) {
        return new ProductSellerPage(
                page.getContent(),
                page.getNumber(),
                page.getSize(),
                page.getTotalElements(),
                page.getTotalPages(),
                page.hasNext()
        );
    }
}
