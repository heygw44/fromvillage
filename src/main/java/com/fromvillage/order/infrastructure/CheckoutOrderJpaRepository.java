package com.fromvillage.order.infrastructure;

import com.fromvillage.order.domain.CheckoutOrder;
import com.fromvillage.order.domain.CheckoutOrderSummaryView;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;

import java.util.Optional;

public interface CheckoutOrderJpaRepository extends JpaRepository<CheckoutOrder, Long> {

    @Query(
            value = """
                    select new com.fromvillage.order.domain.CheckoutOrderSummaryView(
                        checkoutOrder.id,
                        checkoutOrder.status,
                        count(sellerOrder.id),
                        checkoutOrder.totalAmount,
                        checkoutOrder.discountAmount,
                        checkoutOrder.finalAmount,
                        checkoutOrder.completedAt,
                        checkoutOrder.canceledAt,
                        checkoutOrder.createdAt
                    )
                    from CheckoutOrder checkoutOrder
                    left join checkoutOrder.sellerOrders sellerOrder
                    where checkoutOrder.user.id = :userId
                    group by
                        checkoutOrder.id,
                        checkoutOrder.status,
                        checkoutOrder.totalAmount,
                        checkoutOrder.discountAmount,
                        checkoutOrder.finalAmount,
                        checkoutOrder.completedAt,
                        checkoutOrder.canceledAt,
                        checkoutOrder.createdAt
                    """,
            countQuery = """
                    select count(checkoutOrder)
                    from CheckoutOrder checkoutOrder
                    where checkoutOrder.user.id = :userId
                    """
    )
    Page<CheckoutOrderSummaryView> findOrderSummariesByUserId(Long userId, Pageable pageable);

    @Query("""
            select checkoutOrder.user.id
            from CheckoutOrder checkoutOrder
            where checkoutOrder.id = :checkoutOrderId
            """)
    Optional<Long> findOwnerIdById(Long checkoutOrderId);

    @Query("""
            select distinct checkoutOrder
            from CheckoutOrder checkoutOrder
            left join fetch checkoutOrder.sellerOrders sellerOrder
            left join fetch sellerOrder.seller seller
            where checkoutOrder.id = :checkoutOrderId
            """)
    Optional<CheckoutOrder> findByIdWithSellerOrders(Long checkoutOrderId);
}
