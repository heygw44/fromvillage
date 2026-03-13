package com.fromvillage.order.presentation;

import com.fromvillage.order.application.OrderSummaryPage;

import java.util.List;

public record OrderSummaryPageResponse(
        List<OrderSummaryResponse> content,
        int page,
        int size,
        long totalElements,
        int totalPages,
        boolean hasNext
) {

    public static OrderSummaryPageResponse from(OrderSummaryPage page) {
        return new OrderSummaryPageResponse(
                page.content().stream()
                        .map(OrderSummaryResponse::from)
                        .toList(),
                page.page(),
                page.size(),
                page.totalElements(),
                page.totalPages(),
                page.hasNext()
        );
    }
}
