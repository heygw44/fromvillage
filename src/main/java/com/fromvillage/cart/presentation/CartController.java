package com.fromvillage.cart.presentation;

import com.fromvillage.cart.application.CartCommandService;
import com.fromvillage.cart.application.CartItemSummary;
import com.fromvillage.cart.application.CartQueryResult;
import com.fromvillage.cart.application.CartQueryService;
import com.fromvillage.common.response.ApiResponse;
import com.fromvillage.common.security.AuthenticatedUser;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpStatus;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.web.bind.annotation.*;

@RestController
@RequiredArgsConstructor
@RequestMapping("/api/v1/cart-items")
public class CartController {

    private final CartQueryService cartQueryService;
    private final CartCommandService cartCommandService;

    @GetMapping
    public ApiResponse<CartResponse> getCartItems(
            @AuthenticationPrincipal AuthenticatedUser authenticatedUser
    ) {
        CartQueryResult result = cartQueryService.getCartItems(authenticatedUser.getUserId());
        return ApiResponse.success(CartResponse.from(result));
    }

    @PostMapping
    @ResponseStatus(HttpStatus.CREATED)
    public ApiResponse<CartItemResponse> addCartItem(
            @AuthenticationPrincipal AuthenticatedUser authenticatedUser,
            @Valid @RequestBody CartCreateRequest request
    ) {
        CartItemSummary result = cartCommandService.addCartItem(
                authenticatedUser.getUserId(),
                request.toCommand()
        );
        return ApiResponse.success(CartItemResponse.from(result));
    }

    @PatchMapping("/{cartItemId}")
    public ApiResponse<CartItemResponse> updateCartItem(
            @AuthenticationPrincipal AuthenticatedUser authenticatedUser,
            @PathVariable Long cartItemId,
            @Valid @RequestBody CartUpdateRequest request
    ) {
        CartItemSummary result = cartCommandService.updateCartItem(
                authenticatedUser.getUserId(),
                cartItemId,
                request.toCommand()
        );
        return ApiResponse.success(CartItemResponse.from(result));
    }

    @DeleteMapping("/{cartItemId}")
    public ApiResponse<Void> deleteCartItem(
            @AuthenticationPrincipal AuthenticatedUser authenticatedUser,
            @PathVariable Long cartItemId
    ) {
        cartCommandService.deleteCartItem(authenticatedUser.getUserId(), cartItemId);
        return ApiResponse.success(null);
    }
}
