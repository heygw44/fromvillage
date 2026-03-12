package com.fromvillage.order.application;

import com.fromvillage.cart.domain.CartItem;
import com.fromvillage.cart.domain.CartStore;
import com.fromvillage.common.exception.BusinessException;
import com.fromvillage.common.exception.ErrorCode;
import com.fromvillage.order.domain.CheckoutOrder;
import com.fromvillage.order.domain.CheckoutOrderStore;
import com.fromvillage.order.domain.OrderItem;
import com.fromvillage.order.domain.SellerOrder;
import com.fromvillage.product.domain.Product;
import com.fromvillage.product.domain.ProductStatus;
import com.fromvillage.user.domain.User;
import com.fromvillage.user.domain.UserStore;
import lombok.RequiredArgsConstructor;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.Clock;
import java.time.LocalDateTime;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;

@Service
@RequiredArgsConstructor
public class OrderCheckoutService {

    private final UserStore userStore;
    private final CartStore cartStore;
    private final CheckoutOrderStore checkoutOrderStore;
    private final Clock clock;

    @PreAuthorize("hasRole('USER')")
    @Transactional
    public OrderCheckoutResult checkout(Long userId) {
        User user = userStore.findById(userId)
                .orElseThrow(() -> new BusinessException(ErrorCode.USER_NOT_FOUND));

        List<CartItem> cartItems = cartStore.findAllForCheckoutByUserId(user.getId());
        validateCartItems(cartItems);

        decreaseStocks(cartItems);

        CheckoutOrder checkoutOrder = createCheckoutOrder(user, cartItems);
        checkoutOrder.complete(LocalDateTime.now(clock));

        CheckoutOrder savedCheckoutOrder = checkoutOrderStore.save(checkoutOrder);
        cartStore.deleteAll(cartItems);

        return OrderCheckoutResult.from(savedCheckoutOrder);
    }

    private void validateCartItems(List<CartItem> cartItems) {
        if (cartItems.isEmpty()) {
            throw new BusinessException(ErrorCode.ORDER_CHECKOUT_CART_EMPTY);
        }

        for (CartItem cartItem : cartItems) {
            Product product = cartItem.getProduct();
            if (product.isDeleted() || product.getStatus() != ProductStatus.ON_SALE) {
                throw new BusinessException(ErrorCode.CART_PRODUCT_UNAVAILABLE);
            }
        }
    }

    private void decreaseStocks(List<CartItem> cartItems) {
        for (CartItem cartItem : cartItems) {
            cartItem.getProduct().decreaseStock(cartItem.getQuantity());
        }
    }

    private CheckoutOrder createCheckoutOrder(User user, List<CartItem> cartItems) {
        Map<User, List<CartItem>> cartItemsBySeller = cartItems.stream()
                .collect(Collectors.groupingBy(
                        cartItem -> cartItem.getProduct().getSeller(),
                        LinkedHashMap::new,
                        Collectors.toList()
                ));

        List<SellerOrder> sellerOrders = cartItemsBySeller.entrySet().stream()
                .map(entry -> createSellerOrder(entry.getKey(), entry.getValue()))
                .toList();

        return CheckoutOrder.create(user, sellerOrders);
    }

    private SellerOrder createSellerOrder(User seller, List<CartItem> cartItems) {
        List<OrderItem> orderItems = cartItems.stream()
                .map(cartItem -> OrderItem.create(cartItem.getProduct(), cartItem.getQuantity()))
                .toList();

        return SellerOrder.create(seller, orderItems);
    }
}
