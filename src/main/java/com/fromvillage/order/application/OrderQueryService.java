package com.fromvillage.order.application;

import com.fromvillage.common.exception.BusinessException;
import com.fromvillage.common.exception.ErrorCode;
import com.fromvillage.order.domain.CheckoutOrder;
import com.fromvillage.order.domain.CheckoutOrderQueryPort;
import lombok.RequiredArgsConstructor;
import org.springframework.data.domain.Pageable;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.Objects;

@Service
@RequiredArgsConstructor
public class OrderQueryService {

    private final CheckoutOrderQueryPort checkoutOrderQueryPort;

    @PreAuthorize("hasRole('USER')")
    @Transactional(readOnly = true)
    public OrderSummaryPage getOrders(Long userId, Pageable pageable) {
        return OrderSummaryPage.from(
                checkoutOrderQueryPort.findAllByUserId(userId, pageable)
                        .map(OrderSummary::from)
        );
    }

    @PreAuthorize("hasRole('USER')")
    @Transactional(readOnly = true)
    public OrderDetail getOrder(Long userId, Long orderId) {
        CheckoutOrder checkoutOrder = checkoutOrderQueryPort.findById(orderId)
                .orElseThrow(() -> new BusinessException(ErrorCode.ORDER_NOT_FOUND));

        if (!Objects.equals(checkoutOrder.getUser().getId(), userId)) {
            throw new BusinessException(ErrorCode.AUTH_FORBIDDEN);
        }

        return OrderDetail.from(checkoutOrder);
    }
}
