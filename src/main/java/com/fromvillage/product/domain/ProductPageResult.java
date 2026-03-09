package com.fromvillage.product.domain;

import java.util.List;

public record ProductPageResult<T>(
        List<T> content,
        int page,
        int size,
        long totalElements,
        int totalPages,
        boolean hasNext
) {
}
