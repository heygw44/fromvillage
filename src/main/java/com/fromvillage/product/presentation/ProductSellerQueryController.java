package com.fromvillage.product.presentation;

import com.fromvillage.common.response.ApiResponse;
import com.fromvillage.common.security.AuthenticatedUser;
import com.fromvillage.product.application.ProductSellerPage;
import com.fromvillage.product.application.ProductSellerQueryService;
import lombok.RequiredArgsConstructor;
import org.springframework.data.domain.Pageable;
import org.springframework.data.domain.Sort;
import org.springframework.data.web.PageableDefault;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequiredArgsConstructor
@RequestMapping("/api/v1/seller/products")
public class ProductSellerQueryController {

    private final ProductSellerQueryService productSellerQueryService;

    @GetMapping
    public ApiResponse<ProductSellerPageResponse> getSellerProducts(
            @AuthenticationPrincipal AuthenticatedUser authenticatedUser,
            @PageableDefault(sort = "createdAt", direction = Sort.Direction.DESC) Pageable pageable
    ) {
        ProductSellerPage result = productSellerQueryService.getSellerProducts(
                authenticatedUser.getUserId(),
                pageable
        );
        return ApiResponse.success(ProductSellerPageResponse.from(result));
    }
}
