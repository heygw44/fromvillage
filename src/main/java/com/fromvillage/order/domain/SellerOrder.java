package com.fromvillage.order.domain;

import com.fromvillage.common.exception.BusinessException;
import com.fromvillage.common.exception.ErrorCode;
import com.fromvillage.common.persistence.BaseTimeEntity;
import com.fromvillage.user.domain.User;
import jakarta.persistence.CascadeType;
import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.EnumType;
import jakarta.persistence.Enumerated;
import jakarta.persistence.FetchType;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.JoinColumn;
import jakarta.persistence.ManyToOne;
import jakarta.persistence.OneToMany;
import jakarta.persistence.Table;
import lombok.AccessLevel;
import lombok.Getter;
import lombok.NoArgsConstructor;

import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.List;
import java.util.Objects;

@Entity
@Table(name = "seller_order")
@Getter
@NoArgsConstructor(access = AccessLevel.PROTECTED)
public class SellerOrder extends BaseTimeEntity {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "checkout_order_id", nullable = false)
    private CheckoutOrder checkoutOrder;

    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "seller_id", nullable = false)
    private User seller;

    @Enumerated(EnumType.STRING)
    @Column(nullable = false, length = 20)
    private OrderStatus status;

    @Column(name = "total_amount", nullable = false)
    private Long totalAmount;

    @Column(name = "discount_amount", nullable = false)
    private Long discountAmount;

    @Column(name = "final_amount", nullable = false)
    private Long finalAmount;

    @Column(name = "completed_at")
    private LocalDateTime completedAt;

    @Column(name = "canceled_at")
    private LocalDateTime canceledAt;

    @OneToMany(mappedBy = "sellerOrder", cascade = CascadeType.ALL, orphanRemoval = true)
    private List<OrderItem> orderItems = new ArrayList<>();

    private SellerOrder(User seller, List<OrderItem> orderItems) {
        this.seller = Objects.requireNonNull(seller);
        this.status = OrderStatus.CREATED;
        this.discountAmount = 0L;
        this.completedAt = null;
        this.canceledAt = null;

        List<OrderItem> items = requireOrderItems(orderItems);
        items.forEach(this::addOrderItem);

        this.totalAmount = calculateTotalAmount(this.orderItems);
        this.finalAmount = this.totalAmount - this.discountAmount;
    }

    public static SellerOrder create(User seller, List<OrderItem> orderItems) {
        return new SellerOrder(seller, orderItems);
    }

    void assignCheckoutOrder(CheckoutOrder checkoutOrder) {
        this.checkoutOrder = Objects.requireNonNull(checkoutOrder);
    }

    public void complete(LocalDateTime completedAt) {
        if (this.status != OrderStatus.CREATED) {
            throw new BusinessException(ErrorCode.ORDER_STATUS_TRANSITION_INVALID);
        }
        this.status = OrderStatus.COMPLETED;
        this.completedAt = Objects.requireNonNull(completedAt);
    }

    public void cancel(LocalDateTime canceledAt) {
        if (this.status != OrderStatus.COMPLETED) {
            throw new BusinessException(ErrorCode.ORDER_STATUS_TRANSITION_INVALID);
        }
        this.status = OrderStatus.CANCELED;
        this.canceledAt = Objects.requireNonNull(canceledAt);
    }

    private void addOrderItem(OrderItem orderItem) {
        OrderItem item = Objects.requireNonNull(orderItem);
        item.assignSellerOrder(this);
        this.orderItems.add(item);
    }

    private static List<OrderItem> requireOrderItems(List<OrderItem> orderItems) {
        List<OrderItem> items = Objects.requireNonNull(orderItems);
        if (items.isEmpty()) {
            throw new BusinessException(ErrorCode.ORDER_ITEMS_REQUIRED);
        }
        return items;
    }

    private static Long calculateTotalAmount(List<OrderItem> orderItems) {
        return orderItems.stream()
                .map(OrderItem::getLineAmount)
                .reduce(0L, Long::sum);
    }
}
