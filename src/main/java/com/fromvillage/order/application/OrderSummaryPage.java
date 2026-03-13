package com.fromvillage.order.application;

import com.fromvillage.order.domain.OrderPageResult;

import java.util.List;

public record OrderSummaryPage(
        List<OrderSummary> content,
        int page,
        int size,
        long totalElements,
        int totalPages,
        boolean hasNext
) {

    public static OrderSummaryPage from(OrderPageResult<OrderSummary> page) {
        return new OrderSummaryPage(
                page.content(),
                page.page(),
                page.size(),
                page.totalElements(),
                page.totalPages(),
                page.hasNext()
        );
    }
}
