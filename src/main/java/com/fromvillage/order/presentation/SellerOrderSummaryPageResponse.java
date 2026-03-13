package com.fromvillage.order.presentation;

import com.fromvillage.order.application.SellerOrderSummaryPage;

import java.util.List;

public record SellerOrderSummaryPageResponse(
        List<SellerOrderSummaryResponse> content,
        int page,
        int size,
        long totalElements,
        int totalPages,
        boolean hasNext
) {

    public static SellerOrderSummaryPageResponse from(SellerOrderSummaryPage page) {
        return new SellerOrderSummaryPageResponse(
                page.content().stream()
                        .map(SellerOrderSummaryResponse::from)
                        .toList(),
                page.page(),
                page.size(),
                page.totalElements(),
                page.totalPages(),
                page.hasNext()
        );
    }
}
