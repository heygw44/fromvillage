package com.fromvillage.product.domain;

import com.fromvillage.common.exception.BusinessException;
import com.fromvillage.common.exception.ErrorCode;
import com.fromvillage.user.domain.User;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.test.util.ReflectionTestUtils;

import java.time.LocalDateTime;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

class ProductTest {

    @Test
    @DisplayName("판매자는 상품을 생성할 수 있다")
    void createProductWithSeller() {
        User seller = User.createUser("seller@example.com", "encoded-password", "seller");
        seller.approveSeller(java.time.LocalDateTime.of(2026, 3, 9, 10, 0));

        Product product = Product.create(
                seller,
                "감자",
                "해남 햇감자",
                ProductCategory.AGRICULTURE,
                12000L,
                5,
                "https://cdn.example.com/potato.jpg"
        );

        assertThat(product.getSeller()).isEqualTo(seller);
        assertThat(product.getName()).isEqualTo("감자");
        assertThat(product.getDescription()).isEqualTo("해남 햇감자");
        assertThat(product.getCategory()).isEqualTo(ProductCategory.AGRICULTURE);
        assertThat(product.getPrice()).isEqualTo(12000L);
        assertThat(product.getStockQuantity()).isEqualTo(5);
        assertThat(product.getStatus()).isEqualTo(ProductStatus.ON_SALE);
        assertThat(product.getImageUrl()).isEqualTo("https://cdn.example.com/potato.jpg");
        assertThat(product.getDeletedAt()).isNull();
    }

    @Test
    @DisplayName("재고가 0이면 기본 상태는 SOLD_OUT이다")
    void createProductWithZeroStockUsesSoldOut() {
        User seller = User.createUser("seller@example.com", "encoded-password", "seller");
        seller.approveSeller(java.time.LocalDateTime.of(2026, 3, 9, 10, 0));

        Product product = Product.create(
                seller,
                "멸치",
                "남해 멸치",
                ProductCategory.FISHERY,
                9000L,
                0,
                "https://cdn.example.com/anchovy.jpg"
        );

        assertThat(product.getStatus()).isEqualTo(ProductStatus.SOLD_OUT);
    }

    @Test
    @DisplayName("SELLER가 아닌 사용자는 상품을 생성할 수 없다")
    void createProductRejectsNonSeller() {
        User user = User.createUser("user@example.com", "encoded-password", "user");

        assertThatThrownBy(() -> Product.create(
                user,
                "감자",
                "해남 햇감자",
                ProductCategory.AGRICULTURE,
                12000L,
                5,
                "https://cdn.example.com/potato.jpg"
        )).isInstanceOf(BusinessException.class)
                .extracting(exception -> ((BusinessException) exception).getErrorCode())
                .isEqualTo(ErrorCode.PRODUCT_SELLER_ROLE_REQUIRED);
    }

    @Test
    @DisplayName("상품 이미지는 https URL만 허용한다")
    void createProductRejectsNonHttpsImageUrl() {
        User seller = User.createUser("seller@example.com", "encoded-password", "seller");
        seller.approveSeller(java.time.LocalDateTime.of(2026, 3, 9, 10, 0));

        assertThatThrownBy(() -> Product.create(
                seller,
                "감자",
                "해남 햇감자",
                ProductCategory.AGRICULTURE,
                12000L,
                5,
                "http://cdn.example.com/potato.jpg"
        )).isInstanceOf(BusinessException.class)
                .extracting(exception -> ((BusinessException) exception).getErrorCode())
                .isEqualTo(ErrorCode.PRODUCT_IMAGE_URL_INVALID);
    }

    @Test
    @DisplayName("상품 가격은 0보다 커야 한다")
    void createProductRejectsNonPositivePrice() {
        User seller = User.createUser("seller@example.com", "encoded-password", "seller");
        seller.approveSeller(java.time.LocalDateTime.of(2026, 3, 9, 10, 0));

        assertThatThrownBy(() -> Product.create(
                seller,
                "감자",
                "해남 햇감자",
                ProductCategory.AGRICULTURE,
                0L,
                5,
                "https://cdn.example.com/potato.jpg"
        )).isInstanceOf(BusinessException.class)
                .extracting(exception -> ((BusinessException) exception).getErrorCode())
                .isEqualTo(ErrorCode.PRODUCT_PRICE_INVALID);
    }

    @Test
    @DisplayName("상품 재고는 0 이상이어야 한다")
    void createProductRejectsNegativeStockQuantity() {
        User seller = User.createUser("seller@example.com", "encoded-password", "seller");
        seller.approveSeller(java.time.LocalDateTime.of(2026, 3, 9, 10, 0));

        assertThatThrownBy(() -> Product.create(
                seller,
                "감자",
                "해남 햇감자",
                ProductCategory.AGRICULTURE,
                12000L,
                -1,
                "https://cdn.example.com/potato.jpg"
        )).isInstanceOf(BusinessException.class)
                .extracting(exception -> ((BusinessException) exception).getErrorCode())
                .isEqualTo(ErrorCode.PRODUCT_STOCK_QUANTITY_INVALID);
    }

    @Test
    @DisplayName("상품 수정은 필드를 교체하고 재고에 따라 상태를 다시 계산한다")
    void updateProductChangesFieldsAndStatus() {
        User seller = User.createUser("seller@example.com", "encoded-password", "seller");
        seller.approveSeller(LocalDateTime.of(2026, 3, 9, 10, 0));
        Product product = Product.create(
                seller,
                "감자",
                "해남 햇감자",
                ProductCategory.AGRICULTURE,
                12000L,
                5,
                "https://cdn.example.com/potato.jpg"
        );

        product.update(
                "완도 활전복 1kg",
                "완도 산지 직송",
                ProductCategory.FISHERY,
                45000L,
                0,
                "https://cdn.example.com/abalone.jpg"
        );

        assertThat(product.getName()).isEqualTo("완도 활전복 1kg");
        assertThat(product.getDescription()).isEqualTo("완도 산지 직송");
        assertThat(product.getCategory()).isEqualTo(ProductCategory.FISHERY);
        assertThat(product.getPrice()).isEqualTo(45000L);
        assertThat(product.getStockQuantity()).isEqualTo(0);
        assertThat(product.getStatus()).isEqualTo(ProductStatus.SOLD_OUT);
        assertThat(product.getImageUrl()).isEqualTo("https://cdn.example.com/abalone.jpg");
    }

    @Test
    @DisplayName("타인 소유 상품 수정은 AUTH_FORBIDDEN을 반환한다")
    void assertOwnedByRejectsNonOwner() {
        User seller = User.createUser("seller@example.com", "encoded-password", "seller");
        seller.approveSeller(LocalDateTime.of(2026, 3, 9, 10, 0));
        User otherSeller = User.createUser("other@example.com", "encoded-password", "other");
        otherSeller.approveSeller(LocalDateTime.of(2026, 3, 9, 10, 0));
        ReflectionTestUtils.setField(seller, "id", 1L);
        ReflectionTestUtils.setField(otherSeller, "id", 2L);
        Product product = Product.create(
                seller,
                "감자",
                "해남 햇감자",
                ProductCategory.AGRICULTURE,
                12000L,
                5,
                "https://cdn.example.com/potato.jpg"
        );

        assertThatThrownBy(() -> product.assertOwnedBy(otherSeller.getId()))
                .isInstanceOf(BusinessException.class)
                .extracting(exception -> ((BusinessException) exception).getErrorCode())
                .isEqualTo(ErrorCode.AUTH_FORBIDDEN);
    }

    @Test
    @DisplayName("상품 삭제는 deletedAt만 기록하는 soft delete다")
    void softDeleteMarksDeletedAt() {
        User seller = User.createUser("seller@example.com", "encoded-password", "seller");
        seller.approveSeller(LocalDateTime.of(2026, 3, 9, 10, 0));
        Product product = Product.create(
                seller,
                "감자",
                "해남 햇감자",
                ProductCategory.AGRICULTURE,
                12000L,
                5,
                "https://cdn.example.com/potato.jpg"
        );

        LocalDateTime deletedAt = LocalDateTime.of(2026, 3, 10, 12, 0);
        product.softDelete(deletedAt);

        assertThat(product.getDeletedAt()).isEqualTo(deletedAt);
        assertThat(product.getName()).isEqualTo("감자");
        assertThat(product.getStatus()).isEqualTo(ProductStatus.ON_SALE);
    }

    @Test
    @DisplayName("재고를 차감해 0이 되면 상태가 SOLD_OUT로 변경된다")
    void decreaseStockChangesStatusToSoldOut() {
        User seller = User.createUser("seller@example.com", "encoded-password", "seller");
        seller.approveSeller(LocalDateTime.of(2026, 3, 9, 10, 0));

        Product product = Product.create(
                seller,
                "감자",
                "해남 햇감자",
                ProductCategory.AGRICULTURE,
                12000L,
                3,
                "https://cdn.example.com/potato.jpg"
        );
        product.decreaseStock(3);

        assertThat(product.getStockQuantity()).isEqualTo(0);
        assertThat(product.getStatus()).isEqualTo(ProductStatus.SOLD_OUT);
    }

    @Test
    @DisplayName("재고보다 많이 차감하면 PRODUCT_STOCK_INSUFFICIENT를 반환한다")
    void decreaseStockRejectsInsufficientStock() {
        User seller = User.createUser("seller@example.com", "encoded-password", "seller");
        seller.approveSeller(LocalDateTime.of(2026, 3, 9, 10, 0));

        Product product = Product.create(
                seller,
                "감자",
                "해남 햇감자",
                ProductCategory.AGRICULTURE,
                12000L,
                2,
                "https://cdn.example.com/potato.jpg"
        );

        assertThatThrownBy(() -> product.decreaseStock(3))
                .isInstanceOf(BusinessException.class)
                .extracting(exception -> ((BusinessException) exception).getErrorCode())
                .isEqualTo(ErrorCode.PRODUCT_STOCK_INSUFFICIENT);
    }

    @Test
    @DisplayName("0 이하 재고 차감 요청은 PRODUCT_STOCK_QUANTITY_INVALID를 반환한다")
    void decreaseStockRejectsNonPositiveQuantity() {
        User seller = User.createUser("seller@example.com", "encoded-password", "seller");
        seller.approveSeller(LocalDateTime.of(2026, 3, 9, 10, 0));

        Product product = Product.create(
                seller,
                "감자",
                "해남 햇감자",
                ProductCategory.AGRICULTURE,
                12000L,
                2,
                "https://cdn.example.com/potato.jpg"
        );

        assertThatThrownBy(() -> product.decreaseStock(0))
                .isInstanceOf(BusinessException.class)
                .extracting(exception -> ((BusinessException) exception).getErrorCode())
                .isEqualTo(ErrorCode.PRODUCT_STOCK_QUANTITY_INVALID);

        assertThatThrownBy(() -> product.decreaseStock(-1))
                .isInstanceOf(BusinessException.class)
                .extracting(exception -> ((BusinessException) exception).getErrorCode())
                .isEqualTo(ErrorCode.PRODUCT_STOCK_QUANTITY_INVALID);
    }

    @Test
    @DisplayName("재고를 복구해 1 이상이 되면 상태가 ON_SALE로 변경된다")
    void restoreStockChangesStatusToOnSale() {
        User seller = User.createUser("seller@example.com", "encoded-password", "seller");
        seller.approveSeller(LocalDateTime.of(2026, 3, 9, 10, 0));

        Product product = Product.create(
                seller,
                "감자",
                "해남 햇감자",
                ProductCategory.AGRICULTURE,
                12000L,
                0,
                "https://cdn.example.com/potato.jpg"
        );
        product.restoreStock(2);

        assertThat(product.getStockQuantity()).isEqualTo(2);
        assertThat(product.getStatus()).isEqualTo(ProductStatus.ON_SALE);
    }

    @Test
    @DisplayName("0 이하 재고 복구 요청은 PRODUCT_STOCK_QUANTITY_INVALID를 반환한다")
    void restoreStockRejectsNonPositiveQuantity() {
        User seller = User.createUser("seller@example.com", "encoded-password", "seller");
        seller.approveSeller(LocalDateTime.of(2026, 3, 9, 10, 0));

        Product product = Product.create(
                seller,
                "감자",
                "해남 햇감자",
                ProductCategory.AGRICULTURE,
                12000L,
                0,
                "https://cdn.example.com/potato.jpg"
        );

        assertThatThrownBy(() -> product.restoreStock(0))
                .isInstanceOf(BusinessException.class)
                .extracting(exception -> ((BusinessException) exception).getErrorCode())
                .isEqualTo(ErrorCode.PRODUCT_STOCK_QUANTITY_INVALID);

        assertThatThrownBy(() -> product.restoreStock(-1))
                .isInstanceOf(BusinessException.class)
                .extracting(exception -> ((BusinessException) exception).getErrorCode())
                .isEqualTo(ErrorCode.PRODUCT_STOCK_QUANTITY_INVALID);
    }

    @Test
    @DisplayName("재고 복구 중 정수 범위를 초과하면 PRODUCT_STOCK_QUANTITY_OVERFLOW를 반환한다")
    void restoreStockRejectsOverflow() {
        User seller = User.createUser("seller@example.com", "encoded-password", "seller");
        seller.approveSeller(LocalDateTime.of(2026, 3, 9, 10, 0));

        Product product = Product.create(
                seller,
                "감자",
                "해남 햇감자",
                ProductCategory.AGRICULTURE,
                12000L,
                Integer.MAX_VALUE,
                "https://cdn.example.com/potato.jpg"
        );

        assertThatThrownBy(() -> product.restoreStock(1))
                .isInstanceOf(BusinessException.class)
                .extracting(exception -> ((BusinessException) exception).getErrorCode())
                .isEqualTo(ErrorCode.PRODUCT_STOCK_QUANTITY_OVERFLOW);
    }

    @Test
    @DisplayName("soft delete 이후 isDeleted는 true를 반환한다")
    void isDeletedReturnsTrueAfterSoftDelete() {
        User seller = User.createUser("seller@example.com", "encoded-password", "seller");
        seller.approveSeller(LocalDateTime.of(2026, 3, 9, 10, 0));

        Product product = Product.create(
            seller,
            "감자",
            "해남 햇감자",
            ProductCategory.AGRICULTURE,
            12000L,
            5,
            "https://cdn.example.com/potato.jpg"
        );

        product.softDelete(LocalDateTime.of(2026, 3, 10, 12, 0));

        assertThat(product.isDeleted()).isTrue();
    }
}
