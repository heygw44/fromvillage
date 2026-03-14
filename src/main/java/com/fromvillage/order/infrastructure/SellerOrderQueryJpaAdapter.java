package com.fromvillage.order.infrastructure;

import com.fromvillage.order.domain.OrderPageRequest;
import com.fromvillage.order.domain.OrderPageResult;
import com.fromvillage.order.domain.OrderQuerySort;
import com.fromvillage.order.domain.SellerOrder;
import com.fromvillage.order.domain.SellerOrderQueryPort;
import com.fromvillage.order.domain.SellerOrderSummaryView;
import lombok.RequiredArgsConstructor;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Pageable;
import org.springframework.data.domain.Sort;
import org.springframework.stereotype.Component;

import java.util.Optional;

@Component
@RequiredArgsConstructor
public class SellerOrderQueryJpaAdapter implements SellerOrderQueryPort {

    private final SellerOrderJpaRepository sellerOrderJpaRepository;

    @Override
    public OrderPageResult<SellerOrderSummaryView> findSellerOrderSummariesBySellerId(Long sellerId, OrderPageRequest pageRequest) {
        Page<SellerOrderSummaryView> page = sellerOrderJpaRepository.findSellerOrderSummariesBySellerId(
                sellerId,
                toPageable(pageRequest)
        );

        return new OrderPageResult<>(
                page.getContent(),
                page.getNumber(),
                page.getSize(),
                page.getTotalElements(),
                page.getTotalPages(),
                page.hasNext()
        );
    }

    @Override
    public Optional<Long> findSellerIdBySellerOrderId(Long sellerOrderId) {
        return sellerOrderJpaRepository.findSellerIdBySellerOrderId(sellerOrderId);
    }

    @Override
    public Optional<SellerOrder> findDetailBySellerOrderId(Long sellerOrderId) {
        return sellerOrderJpaRepository.findByIdWithCheckoutOrderAndItems(sellerOrderId);
    }

    private Pageable toPageable(OrderPageRequest pageRequest) {
        return PageRequest.of(pageRequest.page(), pageRequest.size(), toSort(pageRequest.sort()));
    }

    private Sort toSort(OrderQuerySort sort) {
        return switch (sort) {
            case CREATED_AT_DESC -> Sort.by(
                    Sort.Order.desc("createdAt"),
                    Sort.Order.desc("id")
            );
            case CREATED_AT_ASC -> Sort.by(
                    Sort.Order.asc("createdAt"),
                    Sort.Order.asc("id")
            );
        };
    }
}
