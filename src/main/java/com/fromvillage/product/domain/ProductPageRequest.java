package com.fromvillage.product.domain;

public record ProductPageRequest(
        int page,
        int size,
        ProductPublicSort sort
) {
}
