package com.fromvillage.order.infrastructure;

import com.fromvillage.order.domain.SellerOrder;
import com.fromvillage.order.domain.SellerOrderSummaryView;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;

import java.util.List;
import java.util.Optional;

public interface SellerOrderJpaRepository extends JpaRepository<SellerOrder, Long> {

    @Query(
            value = """
                    select new com.fromvillage.order.domain.SellerOrderSummaryView(
                        sellerOrder.id,
                        checkoutOrder.orderNumber,
                        buyer.nickname,
                        sellerOrder.status,
                        sellerOrder.totalAmount,
                        sellerOrder.discountAmount,
                        sellerOrder.finalAmount,
                        sellerOrder.completedAt,
                        sellerOrder.canceledAt,
                        sellerOrder.createdAt
                    )
                    from SellerOrder sellerOrder
                    join sellerOrder.checkoutOrder checkoutOrder
                    join checkoutOrder.user buyer
                    where sellerOrder.seller.id = :sellerId
                    """,
            countQuery = """
                    select count(sellerOrder)
                    from SellerOrder sellerOrder
                    where sellerOrder.seller.id = :sellerId
                    """
    )
    Page<SellerOrderSummaryView> findSellerOrderSummariesBySellerId(Long sellerId, Pageable pageable);

    @Query("""
            select sellerOrder.seller.id
            from SellerOrder sellerOrder
            where sellerOrder.id = :sellerOrderId
            """)
    Optional<Long> findSellerIdBySellerOrderId(Long sellerOrderId);

    @Query("""
            select distinct sellerOrder
            from SellerOrder sellerOrder
            join fetch sellerOrder.checkoutOrder checkoutOrder
            join fetch checkoutOrder.user buyer
            left join fetch sellerOrder.orderItems orderItem
            left join fetch orderItem.product product
            join fetch sellerOrder.seller seller
            where sellerOrder.id = :sellerOrderId
            """)
    Optional<SellerOrder> findByIdWithCheckoutOrderAndItems(Long sellerOrderId);

    @Query("""
            select distinct sellerOrder
            from SellerOrder sellerOrder
            join fetch sellerOrder.checkoutOrder checkoutOrder
            left join fetch sellerOrder.orderItems orderItem
            left join fetch orderItem.product product
            join fetch sellerOrder.seller seller
            where checkoutOrder.id = :checkoutOrderId
            """)
    List<SellerOrder> findAllByCheckoutOrderIdWithItems(Long checkoutOrderId);

    @Query("""
            select distinct sellerOrder
            from SellerOrder sellerOrder
            join fetch sellerOrder.checkoutOrder checkoutOrder
            left join fetch sellerOrder.orderItems orderItem
            left join fetch orderItem.product product
            join fetch sellerOrder.seller seller
            where sellerOrder.seller.id = :sellerId
            """)
    List<SellerOrder> findAllBySellerIdWithItems(Long sellerId);
}
