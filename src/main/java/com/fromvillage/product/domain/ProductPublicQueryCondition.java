package com.fromvillage.product.domain;

public record ProductPublicQueryCondition(
        String keyword,
        ProductCategory category
) {
}
