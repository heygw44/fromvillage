package com.fromvillage.order.presentation;

import com.fromvillage.common.response.ApiResponse;
import com.fromvillage.common.security.AuthenticatedUser;
import com.fromvillage.order.application.SellerOrderDetail;
import com.fromvillage.order.application.SellerOrderQueryService;
import com.fromvillage.order.application.SellerOrderSummaryPage;
import lombok.RequiredArgsConstructor;
import org.springframework.data.domain.Pageable;
import org.springframework.data.domain.Sort;
import org.springframework.data.web.PageableDefault;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequiredArgsConstructor
@RequestMapping("/api/v1/seller-orders")
public class SellerOrderController {

    private final SellerOrderQueryService sellerOrderQueryService;

    @GetMapping
    public ApiResponse<SellerOrderSummaryPageResponse> getSellerOrders(
            @AuthenticationPrincipal AuthenticatedUser authenticatedUser,
            @PageableDefault(sort = "createdAt", direction = Sort.Direction.DESC) Pageable pageable
    ) {
        SellerOrderSummaryPage page = sellerOrderQueryService.getSellerOrders(authenticatedUser.getUserId(), pageable);
        return ApiResponse.success(SellerOrderSummaryPageResponse.from(page));
    }

    @GetMapping("/{sellerOrderId}")
    public ApiResponse<SellerOrderDetailResponse> getSellerOrder(
            @AuthenticationPrincipal AuthenticatedUser authenticatedUser,
            @PathVariable Long sellerOrderId
    ) {
        SellerOrderDetail detail = sellerOrderQueryService.getSellerOrder(
                authenticatedUser.getUserId(),
                sellerOrderId
        );
        return ApiResponse.success(SellerOrderDetailResponse.from(detail));
    }
}
