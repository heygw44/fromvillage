package com.fromvillage.order.application;

import com.fromvillage.common.exception.BusinessException;
import com.fromvillage.common.exception.ErrorCode;
import com.fromvillage.order.domain.CheckoutOrder;
import com.fromvillage.product.domain.Product;
import com.fromvillage.product.domain.ProductStore;
import com.fromvillage.user.domain.User;
import com.fromvillage.user.domain.UserStore;
import lombok.RequiredArgsConstructor;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;

@Service
@RequiredArgsConstructor
public class OrderDirectCheckoutService {

    private final UserStore userStore;
    private final ProductStore productStore;
    private final OrderPlacementService orderPlacementService;

    @PreAuthorize("hasRole('USER')")
    @Transactional
    public OrderCheckoutResult directCheckout(Long userId, OrderDirectCheckoutCommand command) {
        User user = userStore.findById(userId)
                .orElseThrow(() -> new BusinessException(ErrorCode.USER_NOT_FOUND));
        Product product = productStore.findById(command.productId())
                .orElseThrow(() -> new BusinessException(ErrorCode.PRODUCT_NOT_FOUND));

        CheckoutOrder checkoutOrder = orderPlacementService.place(
                user,
                List.of(OrderCheckoutLine.of(product, command.quantity()))
        );
        return OrderCheckoutResult.from(checkoutOrder);
    }
}
