package com.fromvillage.order.application;

import com.fromvillage.common.exception.BusinessException;
import com.fromvillage.common.exception.ErrorCode;
import com.fromvillage.order.domain.OrderPageRequest;
import com.fromvillage.order.domain.OrderPageResult;
import com.fromvillage.order.domain.OrderQuerySort;
import com.fromvillage.order.domain.SellerOrder;
import com.fromvillage.order.domain.SellerOrderQueryPort;
import com.fromvillage.order.domain.SellerOrderSummaryView;
import lombok.RequiredArgsConstructor;
import org.springframework.data.domain.Pageable;
import org.springframework.data.domain.Sort;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.Objects;

@Service
@RequiredArgsConstructor
public class SellerOrderQueryService {

    private final SellerOrderQueryPort sellerOrderQueryPort;

    @PreAuthorize("hasRole('SELLER')")
    @Transactional(readOnly = true)
    public SellerOrderSummaryPage getSellerOrders(Long sellerId, Pageable pageable) {
        OrderPageResult<SellerOrderSummaryView> queriedPage =
                sellerOrderQueryPort.findSellerOrderSummariesBySellerId(sellerId, toOrderPageRequest(pageable));
        OrderPageResult<SellerOrderSummary> summaries = new OrderPageResult<>(
                queriedPage.content().stream()
                        .map(SellerOrderSummary::from)
                        .toList(),
                queriedPage.page(),
                queriedPage.size(),
                queriedPage.totalElements(),
                queriedPage.totalPages(),
                queriedPage.hasNext()
        );
        return SellerOrderSummaryPage.from(summaries);
    }

    @PreAuthorize("hasRole('SELLER')")
    @Transactional(readOnly = true)
    public SellerOrderDetail getSellerOrder(Long sellerId, Long sellerOrderId) {
        validateOwnership(sellerId, sellerOrderId);
        SellerOrder sellerOrder = sellerOrderQueryPort.findDetailBySellerOrderId(sellerOrderId)
                .orElseThrow(() -> new BusinessException(ErrorCode.ORDER_NOT_FOUND));
        return SellerOrderDetail.from(sellerOrder);
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

        if (sort.stream().skip(1).findAny().isPresent()) {
            throw new BusinessException(ErrorCode.VALIDATION_ERROR);
        }

        Sort.Order order = sort.stream()
                .findFirst()
                .orElseThrow(() -> new BusinessException(ErrorCode.VALIDATION_ERROR));

        if (!"createdAt".equals(order.getProperty())) {
            throw new BusinessException(ErrorCode.VALIDATION_ERROR);
        }

        return order.isAscending() ? OrderQuerySort.CREATED_AT_ASC : OrderQuerySort.CREATED_AT_DESC;
    }

    private void validateOwnership(Long sellerId, Long sellerOrderId) {
        Long ownerId = sellerOrderQueryPort.findSellerIdBySellerOrderId(sellerOrderId)
                .orElseThrow(() -> new BusinessException(ErrorCode.ORDER_NOT_FOUND));

        if (!Objects.equals(ownerId, sellerId)) {
            throw new BusinessException(ErrorCode.AUTH_FORBIDDEN);
        }
    }
}
