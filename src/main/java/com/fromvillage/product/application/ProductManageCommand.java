package com.fromvillage.product.application;

import com.fromvillage.product.domain.ProductCategory;

public record ProductManageCommand(
        String name,
        String description,
        ProductCategory category,
        Long price,
        Integer stockQuantity,
        String imageUrl
) {
}
