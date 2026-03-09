package com.fromvillage.product.domain;

import com.fromvillage.common.exception.BusinessException;
import com.fromvillage.common.exception.ErrorCode;
import com.fromvillage.common.persistence.BaseTimeEntity;
import com.fromvillage.user.domain.User;
import com.fromvillage.user.domain.UserRole;
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
import jakarta.persistence.Table;
import lombok.AccessLevel;
import lombok.Getter;
import lombok.NoArgsConstructor;

import java.net.URI;
import java.time.LocalDateTime;
import java.util.Objects;

@Entity
@Table(name = "products")
@NoArgsConstructor(access = AccessLevel.PROTECTED)
public class Product extends BaseTimeEntity {

    @Getter
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Getter
    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "seller_id", nullable = false)
    private User seller;

    @Getter
    @Column(nullable = false, length = 100)
    private String name;

    @Getter
    @Column(nullable = false, length = 1000)
    private String description;

    @Getter
    @Enumerated(EnumType.STRING)
    @Column(nullable = false, length = 30)
    private ProductCategory category;

    @Getter
    @Column(nullable = false)
    private Integer price;

    @Getter
    @Column(name = "stock_quantity", nullable = false)
    private Integer stockQuantity;

    @Getter
    @Enumerated(EnumType.STRING)
    @Column(nullable = false, length = 30)
    private ProductStatus status;

    @Getter
    @Column(name = "image_url", nullable = false, length = 500)
    private String imageUrl;

    @Getter
    @Column(name = "deleted_at")
    private LocalDateTime deletedAt;

    private Product(
            User seller,
            String name,
            String description,
            ProductCategory category,
            Integer price,
            Integer stockQuantity,
            String imageUrl,
            ProductStatus status
    ) {
        this.seller = requireSeller(seller);
        this.name = Objects.requireNonNull(name);
        this.description = Objects.requireNonNull(description);
        this.category = Objects.requireNonNull(category);
        this.price = Objects.requireNonNull(price);
        this.stockQuantity = Objects.requireNonNull(stockQuantity);
        this.imageUrl = requireHttpsImageUrl(imageUrl);
        this.status = Objects.requireNonNull(status);
        this.deletedAt = null;
    }

    public static Product create(
            User seller,
            String name,
            String description,
            ProductCategory category,
            Integer price,
            Integer stockQuantity,
            String imageUrl
    ) {
        ProductStatus defaultStatus = Objects.requireNonNull(stockQuantity) == 0
                ? ProductStatus.SOLD_OUT
                : ProductStatus.ON_SALE;

        return new Product(seller, name, description, category, price, stockQuantity, imageUrl, defaultStatus);
    }

    private static User requireSeller(User seller) {
        User owner = Objects.requireNonNull(seller);
        if (owner.getRole() != UserRole.SELLER) {
            throw new BusinessException(ErrorCode.PRODUCT_SELLER_ROLE_REQUIRED);
        }
        return owner;
    }

    private static String requireHttpsImageUrl(String imageUrl) {
        String value = Objects.requireNonNull(imageUrl);
        if (value.isBlank()) {
            throw new BusinessException(ErrorCode.PRODUCT_IMAGE_URL_INVALID);
        }

        try {
            URI uri = URI.create(value);
            if (!"https".equalsIgnoreCase(uri.getScheme())) {
                throw new BusinessException(ErrorCode.PRODUCT_IMAGE_URL_INVALID);
            }
            return value;
        } catch (IllegalArgumentException exception) {
            throw new BusinessException(ErrorCode.PRODUCT_IMAGE_URL_INVALID);
        }
    }
}
