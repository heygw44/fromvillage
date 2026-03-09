package com.fromvillage.product.application;

import com.fromvillage.common.exception.BusinessException;
import com.fromvillage.common.exception.ErrorCode;
import com.fromvillage.product.domain.ProductCategory;
import com.fromvillage.product.domain.ProductPublicQueryPort;
import lombok.RequiredArgsConstructor;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Pageable;
import org.springframework.data.domain.Sort;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
@RequiredArgsConstructor
public class ProductPublicQueryService {

    private final ProductPublicQueryPort productPublicQueryPort;

    @Transactional(readOnly = true)
    public ProductPublicPage getProducts(String keyword, String category, int page, int size, String sort) {
        Pageable pageable = PageRequest.of(page, size, resolveSort(sort));
        ProductCategory productCategory = category == null ? null : ProductCategory.valueOf(category);

        return ProductPublicPage.from(
                productPublicQueryPort.findPublicProducts(normalizeKeyword(keyword), productCategory, pageable)
        );
    }

    @Transactional(readOnly = true)
    public ProductPublicDetail getProduct(Long productId) {
        return productPublicQueryPort.findPublicProductById(productId)
                .map(ProductPublicDetail::from)
                .orElseThrow(() -> new BusinessException(ErrorCode.PRODUCT_NOT_FOUND));
    }

    private String normalizeKeyword(String keyword) {
        if (keyword == null) {
            return null;
        }
        String normalized = keyword.trim();
        return normalized.isEmpty() ? null : normalized;
    }

    private Sort resolveSort(String sort) {
        return switch (sort) {
            case "createdAt,desc" -> Sort.by(
                    Sort.Order.desc("createdAt"),
                    Sort.Order.desc("id")
            );
            case "price,asc" -> Sort.by(
                    Sort.Order.asc("price"),
                    Sort.Order.desc("id")
            );
            case "price,desc" -> Sort.by(
                    Sort.Order.desc("price"),
                    Sort.Order.desc("id")
            );
            default -> throw new BusinessException(ErrorCode.VALIDATION_ERROR);
        };
    }
}
