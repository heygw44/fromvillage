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
import java.util.Locale;
import java.util.Objects;
import java.util.UUID;

@Entity
@Table(name = "checkout_order")
@Getter
@NoArgsConstructor(access = AccessLevel.PROTECTED)
public class CheckoutOrder extends BaseTimeEntity {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(name = "order_number", nullable = false, unique = true, length = 40, updatable = false)
    private String orderNumber;

    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "user_id", nullable = false)
    private User user;

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

    @OneToMany(mappedBy = "checkoutOrder", cascade = CascadeType.ALL, orphanRemoval = true)
    private List<SellerOrder> sellerOrders = new ArrayList<>();

    private CheckoutOrder(User user, List<SellerOrder> sellerOrders) {
        this.orderNumber = generateOrderNumber();
        this.user = Objects.requireNonNull(user);
        this.status = OrderStatus.CREATED;
        this.completedAt = null;
        this.canceledAt = null;

        List<SellerOrder> orders = requireSellerOrders(sellerOrders);
        orders.forEach(this::addSellerOrder);

        this.totalAmount = calculateTotalAmount(this.sellerOrders);
        this.discountAmount = calculateDiscountAmount(this.sellerOrders);
        this.finalAmount = this.totalAmount - this.discountAmount;
    }

    public static CheckoutOrder create(User user, List<SellerOrder> sellerOrders) {
        return new CheckoutOrder(user, sellerOrders);
    }

    public void complete(LocalDateTime completedAt) {
        if (this.status != OrderStatus.CREATED) {
            throw new BusinessException(ErrorCode.ORDER_STATUS_TRANSITION_INVALID);
        }

        LocalDateTime completedTime = Objects.requireNonNull(completedAt);
        this.sellerOrders.forEach(sellerOrder -> sellerOrder.complete(completedTime));
        this.status = OrderStatus.COMPLETED;
        this.completedAt = completedTime;
    }

    public void cancel(LocalDateTime canceledAt) {
        if (this.status != OrderStatus.COMPLETED) {
            throw new BusinessException(ErrorCode.ORDER_STATUS_TRANSITION_INVALID);
        }

        LocalDateTime canceledTime = Objects.requireNonNull(canceledAt);
        this.sellerOrders.forEach(sellerOrder -> sellerOrder.cancel(canceledTime));
        this.status = OrderStatus.CANCELED;
        this.canceledAt = canceledTime;
    }

    private void addSellerOrder(SellerOrder sellerOrder) {
        SellerOrder order = Objects.requireNonNull(sellerOrder);
        order.assignCheckoutOrder(this);
        this.sellerOrders.add(order);
    }

    private static List<SellerOrder> requireSellerOrders(List<SellerOrder> sellerOrders) {
        List<SellerOrder> orders = Objects.requireNonNull(sellerOrders);
        if (orders.isEmpty()) {
            throw new BusinessException(ErrorCode.ORDER_SELLER_ORDERS_REQUIRED);
        }
        return orders;
    }

    private static Long calculateTotalAmount(List<SellerOrder> sellerOrders) {
        return sellerOrders.stream()
                .map(SellerOrder::getTotalAmount)
                .reduce(0L, Math::addExact);
    }

    private static Long calculateDiscountAmount(List<SellerOrder> sellerOrders) {
        return sellerOrders.stream()
                .map(SellerOrder::getDiscountAmount)
                .reduce(0L, Math::addExact);
    }

    private static String generateOrderNumber() {
        return "ORD-" + UUID.randomUUID().toString().replace("-", "").toUpperCase(Locale.ROOT);
    }
}
