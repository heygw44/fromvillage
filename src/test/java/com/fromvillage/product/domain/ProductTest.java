package com.fromvillage.product.domain;

import com.fromvillage.common.exception.BusinessException;
import com.fromvillage.common.exception.ErrorCode;
import com.fromvillage.user.domain.User;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

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
                12000,
                5,
                "https://cdn.example.com/potato.jpg"
        );

        assertThat(product.getSeller()).isEqualTo(seller);
        assertThat(product.getName()).isEqualTo("감자");
        assertThat(product.getDescription()).isEqualTo("해남 햇감자");
        assertThat(product.getCategory()).isEqualTo(ProductCategory.AGRICULTURE);
        assertThat(product.getPrice()).isEqualTo(12000);
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
                9000,
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
                12000,
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
                12000,
                5,
                "http://cdn.example.com/potato.jpg"
        )).isInstanceOf(BusinessException.class)
                .extracting(exception -> ((BusinessException) exception).getErrorCode())
                .isEqualTo(ErrorCode.PRODUCT_IMAGE_URL_INVALID);
    }
}
