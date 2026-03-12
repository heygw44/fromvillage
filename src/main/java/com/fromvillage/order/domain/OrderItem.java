package com.fromvillage.order.domain;

import com.fromvillage.common.exception.BusinessException;
import com.fromvillage.common.exception.ErrorCode;
import com.fromvillage.common.persistence.BaseTimeEntity;
import com.fromvillage.product.domain.Product;
import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.FetchType;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.JoinColumn;
import jakarta.persistence.ManyToOne;
import jakarta.persistence.Table;
import lombok.AccessLevel;
import lombok.Getter;
import lombok.NoArgsConstructor;

import java.util.Objects;

@Entity
@Table(name = "order_item")
@Getter
@NoArgsConstructor(access = AccessLevel.PROTECTED)
public class OrderItem extends BaseTimeEntity {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "seller_order_id", nullable = false)
    private SellerOrder sellerOrder;

    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "product_id", nullable = false)
    private Product product;

    @Column(name = "product_name_snapshot", nullable = false, length = 100)
    private String productNameSnapshot;

    @Column(name = "product_price_snapshot", nullable = false)
    private Long productPriceSnapshot;

    @Column(nullable = false)
    private Integer quantity;

    @Column(name = "line_amount", nullable = false)
    private Long lineAmount;

    private OrderItem(Product product, int quantity) {
        Product source = Objects.requireNonNull(product);
        int orderQuantity = requireQuantity(quantity);

        this.product = source;
        this.productNameSnapshot = source.getName();
        this.productPriceSnapshot = source.getPrice();
        this.quantity = orderQuantity;
        this.lineAmount = Math.multiplyExact(this.productPriceSnapshot, orderQuantity);
    }

    public static OrderItem create(Product product, int quantity) {
        return new OrderItem(product, quantity);
    }

    void assignSellerOrder(SellerOrder sellerOrder) {
        SellerOrder parent = Objects.requireNonNull(sellerOrder);
        if (this.sellerOrder != null && this.sellerOrder != parent) {
            throw new IllegalStateException("Order item is already assigned to another seller order.");
        }
        this.sellerOrder = parent;
    }

    private static int requireQuantity(int quantity) {
        if (quantity < 1) {
            throw new BusinessException(ErrorCode.ORDER_ITEM_QUANTITY_INVALID);
        }
        return quantity;
    }
}
