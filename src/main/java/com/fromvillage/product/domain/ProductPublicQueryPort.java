package com.fromvillage.product.domain;

import java.util.Optional;

public interface ProductPublicQueryPort {

    ProductPageResult<Product> findPublicProducts(ProductPublicQueryCondition condition, ProductPageRequest pageRequest);

    Optional<Product> findPublicProductById(Long productId);
}
