package com.fromvillage.cart.application;

import com.fromvillage.cart.domain.CartItem;
import com.fromvillage.cart.domain.CartStore;
import com.fromvillage.common.exception.BusinessException;
import com.fromvillage.common.exception.ErrorCode;
import com.fromvillage.product.domain.Product;
import com.fromvillage.product.domain.ProductStatus;
import com.fromvillage.product.domain.ProductStore;
import com.fromvillage.user.domain.User;
import com.fromvillage.user.domain.UserStore;
import lombok.RequiredArgsConstructor;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
@RequiredArgsConstructor
public class CartCommandService {

    private final CartStore cartStore;
    private final ProductStore productStore;
    private final UserStore userStore;

    @PreAuthorize("hasRole('USER')")
    @Transactional
    public CartItemSummary addCartItem(Long userId, CartCreateCommand command) {
        User user = userStore.findById(userId)
                .orElseThrow(() -> new BusinessException(ErrorCode.USER_NOT_FOUND));

        Product product = productStore.findById(command.productId())
                .orElseThrow(() -> new BusinessException(ErrorCode.PRODUCT_NOT_FOUND));

        validateAvailableProduct(product);

        CartItem cartItem = cartStore.findByUserIdAndProductId(userId, command.productId())
                .map(existing -> mergeQuantity(existing, command.quantity()))
                .orElseGet(() -> CartItem.create(user, product, command.quantity()));

        return CartItemSummary.from(cartStore.save(cartItem));
    }

    @PreAuthorize("hasRole('USER')")
    @Transactional
    public CartItemSummary updateCartItem(Long userId, Long cartItemId, CartUpdateCommand command) {
        CartItem cartItem = getOwnedCartItem(userId, cartItemId);
        validateAvailableProduct(cartItem.getProduct());
        cartItem.changeQuantity(command.quantity());

        return CartItemSummary.from(cartStore.save(cartItem));
    }

    @PreAuthorize("hasRole('USER')")
    @Transactional
    public void deleteCartItem(Long userId, Long cartItemId) {
        CartItem cartItem = getOwnedCartItem(userId, cartItemId);
        cartStore.delete(cartItem);
    }

    private CartItem getOwnedCartItem(Long userId, Long cartItemId) {
        CartItem cartItem = cartStore.findById(cartItemId)
                .orElseThrow(() -> new BusinessException(ErrorCode.CART_ITEM_NOT_FOUND));

        if (!cartItem.getUser().getId().equals(userId)) {
            throw new BusinessException(ErrorCode.AUTH_FORBIDDEN);
        }

        return cartItem;
    }

    private CartItem mergeQuantity(CartItem cartItem, Integer quantity) {
        cartItem.changeQuantity(cartItem.getQuantity() + quantity);
        return cartItem;
    }

    private void validateAvailableProduct(Product product) {
        if (product.isDeleted() || product.getStatus() != ProductStatus.ON_SALE) {
            throw new BusinessException(ErrorCode.CART_PRODUCT_UNAVAILABLE);
        }
    }
}
