package com.fromvillage.product.presentation;

import com.fromvillage.product.application.ProductManageCommand;
import com.fromvillage.product.domain.ProductCategory;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Pattern;
import jakarta.validation.constraints.Size;

public record ProductManageRequest(
        @NotBlank(message = "상품명이 입력되지 않았습니다.")
        @Size(max = 100, message = "상품명은 100자 이하로 입력해 주세요.")
        String name,

        @NotBlank(message = "상품 설명이 입력되지 않았습니다.")
        String description,

        @NotBlank(message = "카테고리가 입력되지 않았습니다.")
        @Pattern(regexp = "AGRICULTURE|FISHERY", message = "카테고리를 다시 확인해 주세요.")
        String category,

        @NotNull(message = "상품 가격이 입력되지 않았습니다.")
        Long price,

        @NotNull(message = "재고 수량이 입력되지 않았습니다.")
        Integer stockQuantity,

        @NotBlank(message = "상품 이미지 주소가 입력되지 않았습니다.")
        String imageUrl
) {

    ProductManageCommand toCommand() {
        return new ProductManageCommand(
                name,
                description,
                ProductCategory.valueOf(category),
                price,
                stockQuantity,
                imageUrl
        );
    }
}
