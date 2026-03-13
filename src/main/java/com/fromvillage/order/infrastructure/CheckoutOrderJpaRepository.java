package com.fromvillage.order.infrastructure;

import com.fromvillage.order.domain.CheckoutOrder;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;

import java.util.Optional;

public interface CheckoutOrderJpaRepository extends JpaRepository<CheckoutOrder, Long> {

    Page<CheckoutOrder> findAllByUserId(Long userId, Pageable pageable);

    @Query("""
            select distinct checkoutOrder
            from CheckoutOrder checkoutOrder
            left join fetch checkoutOrder.sellerOrders sellerOrder
            left join fetch sellerOrder.seller seller
            where checkoutOrder.id = :checkoutOrderId
            """)
    Optional<CheckoutOrder> findByIdWithSellerOrders(Long checkoutOrderId);
}
