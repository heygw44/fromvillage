package com.fromvillage.order.domain;

import java.util.List;

public record OrderPageResult<T>(
        List<T> content,
        int page,
        int size,
        long totalElements,
        int totalPages,
        boolean hasNext
) {
}
