package com.fromvillage.cart.application;

import com.fromvillage.cart.domain.CartStore;
import lombok.RequiredArgsConstructor;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;

@Service
@RequiredArgsConstructor
public class CartQueryService {

    private final CartStore cartStore;

    @PreAuthorize("hasRole('USER')")
    @Transactional(readOnly = true)
    public CartQueryResult getCartItems(Long userId) {
        List<CartItemSummary> items = cartStore.findAllActiveByUserId(userId).stream()
                .map(CartItemSummary::from)
                .toList();

        int totalQuantity = items.stream()
                .mapToInt(CartItemSummary::quantity)
                .sum();

        long totalAmount = items.stream()
                .mapToLong(CartItemSummary::lineAmount)
                .sum();

        return new CartQueryResult(
                items,
                items.size(),
                totalQuantity,
                totalAmount
        );
    }
}
