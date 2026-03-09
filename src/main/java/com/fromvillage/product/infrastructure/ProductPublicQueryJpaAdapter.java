package com.fromvillage.product.infrastructure;

import com.fromvillage.product.domain.Product;
import com.fromvillage.product.domain.ProductPageRequest;
import com.fromvillage.product.domain.ProductPageResult;
import com.fromvillage.product.domain.ProductPublicQueryCondition;
import com.fromvillage.product.domain.ProductPublicQueryPort;
import com.fromvillage.product.domain.ProductPublicSort;
import lombok.RequiredArgsConstructor;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Pageable;
import org.springframework.data.domain.Sort;
import org.springframework.stereotype.Component;

import java.util.Optional;

@Component
@RequiredArgsConstructor
public class ProductPublicQueryJpaAdapter implements ProductPublicQueryPort {

    private final ProductJpaRepository productJpaRepository;

    @Override
    public ProductPageResult<Product> findPublicProducts(
            ProductPublicQueryCondition condition,
            ProductPageRequest pageRequest
    ) {
        Page<Product> result = productJpaRepository.findPublicProducts(
                condition.keyword(),
                condition.category(),
                toPageable(pageRequest)
        );

        return new ProductPageResult<>(
                result.getContent(),
                result.getNumber(),
                result.getSize(),
                result.getTotalElements(),
                result.getTotalPages(),
                result.hasNext()
        );
    }

    @Override
    public Optional<Product> findPublicProductById(Long productId) {
        return productJpaRepository.findByIdAndDeletedAtIsNull(productId);
    }

    private Pageable toPageable(ProductPageRequest pageRequest) {
        return PageRequest.of(pageRequest.page(), pageRequest.size(), toSort(pageRequest.sort()));
    }

    private Sort toSort(ProductPublicSort sort) {
        return switch (sort) {
            case CREATED_AT_DESC -> Sort.by(
                    Sort.Order.desc("createdAt"),
                    Sort.Order.desc("id")
            );
            case PRICE_ASC -> Sort.by(
                    Sort.Order.asc("price"),
                    Sort.Order.desc("id")
            );
            case PRICE_DESC -> Sort.by(
                    Sort.Order.desc("price"),
                    Sort.Order.desc("id")
            );
        };
    }
}
