package com.fromvillage.order.infrastructure;

import com.fromvillage.order.domain.SellerOrder;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;

import java.util.List;
import java.util.Optional;

public interface SellerOrderJpaRepository extends JpaRepository<SellerOrder, Long> {

    @Query("""
            select distinct sellerOrder
            from SellerOrder sellerOrder
            join fetch sellerOrder.checkoutOrder checkoutOrder
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
