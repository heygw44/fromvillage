package com.fromvillage.product.presentation;

import com.fromvillage.common.response.ApiResponse;
import com.fromvillage.common.security.AuthenticatedUser;
import com.fromvillage.product.application.ProductManageResult;
import com.fromvillage.product.application.ProductManageService;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpStatus;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.web.bind.annotation.DeleteMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.PutMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.ResponseStatus;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequiredArgsConstructor
@RequestMapping("/api/v1/products")
public class ProductManageController {

    private final ProductManageService productManageService;

    @PostMapping
    @ResponseStatus(HttpStatus.CREATED)
    public ApiResponse<ProductManageResponse> createProduct(
            @AuthenticationPrincipal AuthenticatedUser authenticatedUser,
            @Valid @RequestBody ProductManageRequest request
    ) {
        ProductManageResult result = productManageService.createProduct(authenticatedUser.getUserId(), request.toCommand());
        return ApiResponse.success(ProductManageResponse.from(result));
    }

    @PutMapping("/{productId}")
    public ApiResponse<ProductManageResponse> updateProduct(
            @AuthenticationPrincipal AuthenticatedUser authenticatedUser,
            @PathVariable Long productId,
            @Valid @RequestBody ProductManageRequest request
    ) {
        ProductManageResult result = productManageService.updateProduct(
                authenticatedUser.getUserId(),
                productId,
                request.toCommand()
        );
        return ApiResponse.success(ProductManageResponse.from(result));
    }

    @DeleteMapping("/{productId}")
    public ApiResponse<Void> deleteProduct(
            @AuthenticationPrincipal AuthenticatedUser authenticatedUser,
            @PathVariable Long productId
    ) {
        productManageService.deleteProduct(authenticatedUser.getUserId(), productId);
        return ApiResponse.success(null);
    }
}
