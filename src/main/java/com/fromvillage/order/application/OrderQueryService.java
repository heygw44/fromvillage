package com.fromvillage.order.application;

import com.fromvillage.common.exception.BusinessException;
import com.fromvillage.common.exception.ErrorCode;
import com.fromvillage.order.domain.CheckoutOrder;
import com.fromvillage.order.domain.CheckoutOrderQueryPort;
import com.fromvillage.order.domain.CheckoutOrderSummaryView;
import com.fromvillage.order.domain.OrderPageRequest;
import com.fromvillage.order.domain.OrderPageResult;
import com.fromvillage.order.domain.OrderQuerySort;
import lombok.RequiredArgsConstructor;
import org.springframework.data.domain.Pageable;
import org.springframework.data.domain.Sort;
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
        OrderPageResult<CheckoutOrderSummaryView> queriedPage =
                checkoutOrderQueryPort.findOrderSummariesByUserId(userId, toOrderPageRequest(pageable));
        OrderPageResult<OrderSummary> summaries = new OrderPageResult<>(
                queriedPage.content().stream()
                        .map(OrderSummary::from)
                        .toList(),
                queriedPage.page(),
                queriedPage.size(),
                queriedPage.totalElements(),
                queriedPage.totalPages(),
                queriedPage.hasNext()
        );
        return OrderSummaryPage.from(summaries);
    }

    @PreAuthorize("hasRole('USER')")
    @Transactional(readOnly = true)
    public OrderDetail getOrder(Long userId, Long orderId) {
        CheckoutOrder checkoutOrder = checkoutOrderQueryPort.findDetailById(orderId)
                .orElseThrow(() -> new BusinessException(ErrorCode.ORDER_NOT_FOUND));

        if (!Objects.equals(checkoutOrder.getUser().getId(), userId)) {
            throw new BusinessException(ErrorCode.AUTH_FORBIDDEN);
        }

        return OrderDetail.from(checkoutOrder);
    }

    private OrderPageRequest toOrderPageRequest(Pageable pageable) {
        return new OrderPageRequest(
                pageable.getPageNumber(),
                pageable.getPageSize(),
                resolveSort(pageable.getSort())
        );
    }

    private OrderQuerySort resolveSort(Sort sort) {
        if (sort.isUnsorted()) {
            return OrderQuerySort.CREATED_AT_DESC;
        }

        Sort.Order order = sort.stream()
                .findFirst()
                .orElseThrow(() -> new BusinessException(ErrorCode.VALIDATION_ERROR));

        if (!"createdAt".equals(order.getProperty())) {
            throw new BusinessException(ErrorCode.VALIDATION_ERROR);
        }

        return order.isAscending() ? OrderQuerySort.CREATED_AT_ASC : OrderQuerySort.CREATED_AT_DESC;
    }
}
