package com.fromvillage.order.application;

import com.fromvillage.cart.domain.CartItem;
import com.fromvillage.common.exception.BusinessException;
import com.fromvillage.common.exception.ErrorCode;
import com.fromvillage.product.domain.Product;

import java.util.Objects;

public record OrderCheckoutLine(
        Product product,
        int quantity
) {

    public OrderCheckoutLine {
        Objects.requireNonNull(product);
        if (quantity < 1) {
            throw new BusinessException(ErrorCode.ORDER_ITEM_QUANTITY_INVALID);
        }
    }

    public static OrderCheckoutLine fromCartItem(CartItem cartItem) {
        CartItem source = Objects.requireNonNull(cartItem);
        return new OrderCheckoutLine(source.getProduct(), source.getQuantity());
    }

    public static OrderCheckoutLine of(Product product, int quantity) {
        return new OrderCheckoutLine(product, quantity);
    }
}
