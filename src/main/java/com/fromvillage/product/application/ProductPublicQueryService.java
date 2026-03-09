package com.fromvillage.product.application;

import com.fromvillage.common.exception.BusinessException;
import com.fromvillage.common.exception.ErrorCode;
import com.fromvillage.product.domain.ProductCategory;
import com.fromvillage.product.domain.ProductPageRequest;
import com.fromvillage.product.domain.ProductPublicQueryCondition;
import com.fromvillage.product.domain.ProductPublicQueryPort;
import com.fromvillage.product.domain.ProductPublicSort;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
@RequiredArgsConstructor
public class ProductPublicQueryService {

    private final ProductPublicQueryPort productPublicQueryPort;

    @Transactional(readOnly = true)
    public ProductPublicPage getProducts(String keyword, String category, int page, int size, String sort) {
        ProductCategory productCategory = category == null ? null : ProductCategory.valueOf(category);

        return ProductPublicPage.from(
                productPublicQueryPort.findPublicProducts(
                        new ProductPublicQueryCondition(normalizeKeyword(keyword), productCategory),
                        new ProductPageRequest(page, size, resolveSort(sort))
                )
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

    private ProductPublicSort resolveSort(String sort) {
        return switch (sort) {
            case "createdAt,desc" -> ProductPublicSort.CREATED_AT_DESC;
            case "price,asc" -> ProductPublicSort.PRICE_ASC;
            case "price,desc" -> ProductPublicSort.PRICE_DESC;
            default -> throw new BusinessException(ErrorCode.VALIDATION_ERROR);
        };
    }
}
