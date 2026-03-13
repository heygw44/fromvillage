package com.fromvillage.order.presentation;

import com.fromvillage.common.response.ApiResponse;
import com.fromvillage.common.security.AuthenticatedUser;
import com.fromvillage.order.application.OrderDirectCheckoutService;
import com.fromvillage.order.application.OrderCheckoutResult;
import com.fromvillage.order.application.OrderCheckoutService;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpStatus;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.ResponseStatus;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequiredArgsConstructor
@RequestMapping("/api/v1/orders")
public class OrderCheckoutController {

    private final OrderCheckoutService orderCheckoutService;
    private final OrderDirectCheckoutService orderDirectCheckoutService;

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
