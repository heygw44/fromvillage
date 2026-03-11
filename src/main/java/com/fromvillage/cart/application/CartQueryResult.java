package com.fromvillage.cart.application;

import java.util.List;

public record CartQueryResult(
        List<CartItemSummary> items,
        int totalItemCount,
        int totalQuantity,
        long totalAmount
) {
}
