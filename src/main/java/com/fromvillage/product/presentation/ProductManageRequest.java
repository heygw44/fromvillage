package com.fromvillage.product.presentation;

import com.fromvillage.product.application.ProductManageCommand;
import com.fromvillage.product.domain.ProductCategory;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Positive;
import jakarta.validation.constraints.PositiveOrZero;
import jakarta.validation.constraints.Size;

public record ProductManageRequest(
        @NotBlank(message = "상품명이 입력되지 않았습니다.")
        @Size(max = 100, message = "상품명은 100자 이하로 입력해 주세요.")
        String name,

        @NotBlank(message = "상품 설명이 입력되지 않았습니다.")
        String description,

        @NotNull(message = "카테고리가 입력되지 않았습니다.")
        ProductCategory category,

        @NotNull(message = "상품 가격이 입력되지 않았습니다.")
        @Positive(message = "상품 가격은 1원 이상이어야 합니다.")
        Long price,

        @NotNull(message = "재고 수량이 입력되지 않았습니다.")
        @PositiveOrZero(message = "재고 수량은 0 이상이어야 합니다.")
        Integer stockQuantity,

        @NotBlank(message = "상품 이미지 주소가 입력되지 않았습니다.")
        String imageUrl
) {

    ProductManageCommand toCommand() {
        return new ProductManageCommand(
                name,
                description,
                category,
                price,
                stockQuantity,
                imageUrl
        );
    }
}
