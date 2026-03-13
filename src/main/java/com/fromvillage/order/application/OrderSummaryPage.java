package com.fromvillage.order.application;

import org.springframework.data.domain.Page;

import java.util.List;

public record OrderSummaryPage(
        List<OrderSummary> content,
        int page,
        int size,
        long totalElements,
        int totalPages,
        boolean hasNext
) {

    public static OrderSummaryPage from(Page<OrderSummary> page) {
        return new OrderSummaryPage(
                page.getContent(),
                page.getNumber(),
                page.getSize(),
                page.getTotalElements(),
                page.getTotalPages(),
                page.hasNext()
        );
    }
}
