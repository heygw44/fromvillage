package com.fromvillage.order.infrastructure;

import com.fromvillage.order.domain.CheckoutOrder;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;

import java.util.Optional;

public interface CheckoutOrderJpaRepository extends JpaRepository<CheckoutOrder, Long> {

    @Query("""
            select distinct checkoutOrder
            from CheckoutOrder checkoutOrder
            left join fetch checkoutOrder.sellerOrders sellerOrder
            left join fetch sellerOrder.seller seller
            where checkoutOrder.id = :checkoutOrderId
            """)
    Optional<CheckoutOrder> findByIdWithSellerOrders(Long checkoutOrderId);
}
