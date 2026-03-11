package com.fromvillage.cart.domain;

import com.fromvillage.common.exception.BusinessException;
import com.fromvillage.common.exception.ErrorCode;
import com.fromvillage.common.persistence.BaseTimeEntity;
import com.fromvillage.product.domain.Product;
import com.fromvillage.user.domain.User;
import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.FetchType;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.JoinColumn;
import jakarta.persistence.ManyToOne;
import jakarta.persistence.Table;
import jakarta.persistence.UniqueConstraint;
import lombok.AccessLevel;
import lombok.Getter;
import lombok.NoArgsConstructor;

import java.util.Objects;

@Entity
@Table(
        name = "cart_item",
        uniqueConstraints = @UniqueConstraint(
                name = "uk_cart_item_user_product",
                columnNames = {"user_id", "product_id"}
        )
)
@Getter
@NoArgsConstructor(access = AccessLevel.PROTECTED)
public class CartItem extends BaseTimeEntity {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "user_id", nullable = false)
    private User user;

    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "product_id", nullable = false)
    private Product product;

    @Column(nullable = false)
    private Integer quantity;

    private CartItem(User user, Product product, Integer quantity) {
        this.user = Objects.requireNonNull(user);
        this.product = Objects.requireNonNull(product);
        this.quantity = requireQuantity(quantity);
    }

    public static CartItem create(User user, Product product, Integer quantity) {
        return new CartItem(user, product, quantity);
    }

    public void changeQuantity(Integer quantity) {
        this.quantity = requireQuantity(quantity);
    }

    private static Integer requireQuantity(Integer quantity) {
        Integer value = Objects.requireNonNull(quantity);
        if (value < 1) {
            throw new BusinessException(ErrorCode.CART_QUANTITY_INVALID);
        }
        return value;
    }
}
