package com.fromvillage.product.application;

import com.fromvillage.common.exception.BusinessException;
import com.fromvillage.common.exception.ErrorCode;
import com.fromvillage.product.domain.Product;
import com.fromvillage.product.domain.ProductStore;
import com.fromvillage.user.domain.User;
import com.fromvillage.user.domain.UserStore;
import lombok.RequiredArgsConstructor;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.Clock;
import java.time.LocalDateTime;

@Service
@RequiredArgsConstructor
public class ProductManageService {

    private final ProductStore productStore;
    private final UserStore userStore;
    private final Clock clock;

    @PreAuthorize("hasRole('SELLER')")
    @Transactional
    public ProductManageResult createProduct(Long sellerId, ProductManageCommand command) {
        User seller = userStore.findById(sellerId)
                .orElseThrow(() -> new BusinessException(ErrorCode.USER_NOT_FOUND));

        Product product = Product.create(
                seller,
                command.name(),
                command.description(),
                command.category(),
                command.price(),
                command.stockQuantity(),
                command.imageUrl()
        );

        return ProductManageResult.from(productStore.save(product));
    }

    @PreAuthorize("hasRole('SELLER')")
    @Transactional
    public ProductManageResult updateProduct(Long sellerId, Long productId, ProductManageCommand command) {
        Product product = getActiveProduct(productId);
        product.assertOwnedBy(sellerId);
        product.update(
                command.name(),
                command.description(),
                command.category(),
                command.price(),
                command.stockQuantity(),
                command.imageUrl()
        );
        return ProductManageResult.from(productStore.save(product));
    }

    @PreAuthorize("hasRole('SELLER')")
    @Transactional
    public void deleteProduct(Long sellerId, Long productId) {
        Product product = getActiveProduct(productId);
        product.assertOwnedBy(sellerId);
        product.softDelete(LocalDateTime.now(clock));
        productStore.save(product);
    }

    private Product getActiveProduct(Long productId) {
        return productStore.findById(productId)
                .filter(product -> !product.isDeleted())
                .orElseThrow(() -> new BusinessException(ErrorCode.PRODUCT_NOT_FOUND));
    }
}
