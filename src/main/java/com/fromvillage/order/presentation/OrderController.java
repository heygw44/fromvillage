package com.fromvillage.order.presentation;

import com.fromvillage.common.response.ApiResponse;
import com.fromvillage.common.security.AuthenticatedUser;
import com.fromvillage.order.application.OrderCancelService;
import com.fromvillage.order.application.OrderDetail;
import com.fromvillage.order.application.OrderDirectCheckoutService;
import com.fromvillage.order.application.OrderCheckoutResult;
import com.fromvillage.order.application.OrderCheckoutService;
import com.fromvillage.order.application.OrderQueryService;
import com.fromvillage.order.application.OrderSummary;
import com.fromvillage.order.application.OrderSummaryPage;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.data.domain.Pageable;
import org.springframework.data.domain.Sort;
import org.springframework.data.web.PageableDefault;
import org.springframework.http.HttpStatus;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.ResponseStatus;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequiredArgsConstructor
@RequestMapping("/api/v1/orders")
public class OrderController {

    private final OrderCheckoutService orderCheckoutService;
    private final OrderDirectCheckoutService orderDirectCheckoutService;
    private final OrderQueryService orderQueryService;
    private final OrderCancelService orderCancelService;

    @GetMapping
    public ApiResponse<OrderSummaryPageResponse> getOrders(
            @AuthenticationPrincipal AuthenticatedUser authenticatedUser,
            @PageableDefault(sort = "createdAt", direction = Sort.Direction.DESC) Pageable pageable
    ) {
        OrderSummaryPage page = orderQueryService.getOrders(authenticatedUser.getUserId(), pageable);
        return ApiResponse.success(OrderSummaryPageResponse.from(page));
    }

    @GetMapping("/{orderId}")
    public ApiResponse<OrderDetailResponse> getOrder(
            @AuthenticationPrincipal AuthenticatedUser authenticatedUser,
            @PathVariable Long orderId
    ) {
        OrderDetail detail = orderQueryService.getOrder(authenticatedUser.getUserId(), orderId);
        return ApiResponse.success(OrderDetailResponse.from(detail));
    }

    @PostMapping("/{orderId}/cancel")
    public ApiResponse<OrderSummaryResponse> cancelOrder(
            @AuthenticationPrincipal AuthenticatedUser authenticatedUser,
            @PathVariable Long orderId
    ) {
        OrderSummary summary = orderCancelService.cancel(authenticatedUser.getUserId(), orderId);
        return ApiResponse.success(OrderSummaryResponse.from(summary));
    }

    @PostMapping("/checkout")
    @ResponseStatus(HttpStatus.CREATED)
    public ApiResponse<OrderCheckoutResponse> checkout(
            @AuthenticationPrincipal AuthenticatedUser authenticatedUser
    ) {
        OrderCheckoutResult result = orderCheckoutService.checkout(authenticatedUser.getUserId());
        return ApiResponse.success(OrderCheckoutResponse.from(result));
    }

    @PostMapping("/direct-checkout")
    @ResponseStatus(HttpStatus.CREATED)
    public ApiResponse<OrderCheckoutResponse> directCheckout(
            @AuthenticationPrincipal AuthenticatedUser authenticatedUser,
            @Valid @RequestBody OrderDirectCheckoutRequest request
    ) {
        OrderCheckoutResult result = orderDirectCheckoutService.directCheckout(
                authenticatedUser.getUserId(),
                request.toCommand()
        );
        return ApiResponse.success(OrderCheckoutResponse.from(result));
    }
}
