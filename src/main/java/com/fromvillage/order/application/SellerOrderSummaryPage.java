package com.fromvillage.order.application;

import com.fromvillage.order.domain.OrderPageResult;

import java.util.List;

public record SellerOrderSummaryPage(
        List<SellerOrderSummary> content,
        int page,
        int size,
        long totalElements,
        int totalPages,
        boolean hasNext
) {

    public static SellerOrderSummaryPage from(OrderPageResult<SellerOrderSummary> page) {
        return new SellerOrderSummaryPage(
                page.content(),
                page.page(),
                page.size(),
                page.totalElements(),
                page.totalPages(),
                page.hasNext()
        );
    }
}
