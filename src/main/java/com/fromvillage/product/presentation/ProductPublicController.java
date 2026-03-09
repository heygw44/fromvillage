package com.fromvillage.product.presentation;

import com.fromvillage.common.response.ApiResponse;
import com.fromvillage.product.application.ProductPublicQueryService;
import jakarta.validation.constraints.Max;
import jakarta.validation.constraints.Min;
import jakarta.validation.constraints.Pattern;
import lombok.RequiredArgsConstructor;
import org.springframework.validation.annotation.Validated;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

@Validated
@RestController
@RequiredArgsConstructor
@RequestMapping("/api/v1/products")
public class ProductPublicController {

    private static final String CATEGORY_PATTERN = "AGRICULTURE|FISHERY";

    private final ProductPublicQueryService productPublicQueryService;

    @GetMapping
    public ApiResponse<ProductPublicPageResponse> getProducts(
            @RequestParam(required = false) String keyword,
            @RequestParam(required = false)
            @Pattern(regexp = CATEGORY_PATTERN, message = "카테고리를 다시 확인해 주세요.")
            String category,
            @RequestParam(defaultValue = "0")
            @Min(value = 0, message = "페이지 번호는 0 이상이어야 합니다.")
            int page,
            @RequestParam(defaultValue = "20")
            @Min(value = 1, message = "페이지 크기는 1 이상이어야 합니다.")
            @Max(value = 100, message = "페이지 크기는 100 이하여야 합니다.")
            int size,
            @RequestParam(defaultValue = "createdAt,desc") String sort
    ) {
        return ApiResponse.success(ProductPublicPageResponse.from(
                productPublicQueryService.getProducts(keyword, category, page, size, sort)
        ));
    }

    @GetMapping("/{productId}")
    public ApiResponse<ProductPublicDetailResponse> getProduct(@PathVariable Long productId) {
        return ApiResponse.success(ProductPublicDetailResponse.from(
                productPublicQueryService.getProduct(productId)
        ));
    }
}
