package com.fromvillage.product.application;

import com.fromvillage.product.domain.ProductStore;
import lombok.RequiredArgsConstructor;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Pageable;
import org.springframework.data.domain.Sort;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
@RequiredArgsConstructor
public class ProductSellerQueryService {

    private final ProductStore productStore;

    @PreAuthorize("hasRole('SELLER')")
    @Transactional(readOnly = true)
    public ProductSellerPage getSellerProducts(Long sellerId, Pageable pageable) {
        var pageRequest = normalize(pageable);

        var page = productStore.findAllBySellerId(sellerId, pageRequest)
                .map(ProductSellerSummary::from);

        return ProductSellerPage.from(page);
    }

    private Pageable normalize(Pageable pageable) {
        if (pageable == null || pageable.isUnpaged()) {
            return PageRequest.of(0, 20, Sort.by(Sort.Direction.DESC, "createdAt"));
        }

        if (pageable.getSort().isUnsorted()) {
            return PageRequest.of(
                    pageable.getPageNumber(),
                    pageable.getPageSize(),
                    Sort.by(Sort.Direction.DESC, "createdAt")
            );
        }

        return pageable;
    }
}
