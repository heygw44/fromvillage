package com.fromvillage.product.infrastructure;

import com.fromvillage.common.config.JpaAuditingConfig;
import com.fromvillage.product.domain.Product;
import com.fromvillage.product.domain.ProductCategory;
import com.fromvillage.product.domain.ProductStore;
import com.fromvillage.product.domain.ProductStatus;
import com.fromvillage.support.TestContainersConfig;
import com.fromvillage.user.domain.User;
import com.fromvillage.user.infrastructure.UserJpaRepository;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.orm.jpa.DataJpaTest;
import org.springframework.context.annotation.Import;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Sort;
import org.springframework.test.context.ActiveProfiles;

import static org.assertj.core.api.Assertions.assertThat;

@DataJpaTest
@ActiveProfiles("test")
@Import({TestContainersConfig.class, JpaAuditingConfig.class, ProductStoreJpaAdapter.class})
class ProductRepositoryTest {

    @Autowired
    private ProductStore productStore;

    @Autowired
    private ProductJpaRepository productJpaRepository;

    @Autowired
    private UserJpaRepository userJpaRepository;

    @Test
    @DisplayName("상품을 저장하고 ID로 조회할 수 있다")
    void saveAndFindById() {
        User seller = createSeller("seller@example.com", "seller");
        Product product = Product.create(
                seller,
                "감자",
                "해남 햇감자",
                ProductCategory.AGRICULTURE,
                12000L,
                5,
                "https://cdn.example.com/potato.jpg"
        );

        Product savedProduct = productStore.save(product);

        assertThat(savedProduct.getId()).isNotNull();
        assertThat(productStore.findById(savedProduct.getId()))
                .isPresent()
                .get()
                .extracting(Product::getName, Product::getStatus)
                .containsExactly("감자", ProductStatus.ON_SALE);
    }

    @Test
    @DisplayName("판매자 기준으로 삭제된 상품 포함하여 페이지 조회할 수 있다")
    void findAllBySellerIdIncludingDeleted() {
        User seller = createSeller("seller1@example.com", "seller1");
        User otherSeller = createSeller("seller2@example.com", "seller2");
        productStore.save(Product.create(
                seller,
                "감자",
                "해남 햇감자",
                ProductCategory.AGRICULTURE,
                12000L,
                5,
                "https://cdn.example.com/potato.jpg"
        ));
        Product deletedProduct = productStore.save(Product.create(
                seller,
                "배추",
                "제주 월동배추",
                ProductCategory.AGRICULTURE,
                15000L,
                0,
                "https://cdn.example.com/cabbage.jpg"
        ));
        deletedProduct.softDelete(java.time.LocalDateTime.of(2026, 3, 10, 12, 0));
        productStore.save(deletedProduct);
        productStore.save(Product.create(
                otherSeller,
                "멸치",
                "남해 멸치",
                ProductCategory.FISHERY,
                9000L,
                3,
                "https://cdn.example.com/anchovy.jpg"
        ));

        var page = productStore.findAllBySellerIdIncludingDeleted(
                seller.getId(),
                PageRequest.of(0, 10, Sort.by("createdAt").ascending())
        );

        assertThat(page.getContent()).hasSize(2);
        assertThat(page.getContent())
                .extracting(Product::getName)
                .containsExactly("감자", "배추");
        assertThat(page.getContent())
                .filteredOn(Product::isDeleted)
                .extracting(Product::getName)
                .containsExactly("배추");
    }

    @Test
    @DisplayName("상품 저장 시 연관관계와 auditing 필드가 기록된다")
    void saveRecordsSellerAndAuditingFields() {
        User seller = createSeller("seller@example.com", "seller");

        Product savedProduct = productStore.save(Product.create(
                seller,
                "멸치",
                "남해 멸치",
                ProductCategory.FISHERY,
                9000L,
                0,
                "https://cdn.example.com/anchovy.jpg"
        ));

        Product persistedProduct = productJpaRepository.findById(savedProduct.getId()).orElseThrow();

        assertThat(persistedProduct.getSeller().getId()).isEqualTo(seller.getId());
        assertThat(persistedProduct.getStatus()).isEqualTo(ProductStatus.SOLD_OUT);
        assertThat(persistedProduct.getCreatedAt()).isNotNull();
        assertThat(persistedProduct.getUpdatedAt()).isNotNull();
        assertThat(persistedProduct.getDeletedAt()).isNull();
    }

    private User createSeller(String email, String nickname) {
        User seller = User.createUser(email, "encoded-password", nickname);
        seller.approveSeller(java.time.LocalDateTime.of(2026, 3, 9, 10, 0));
        return userJpaRepository.saveAndFlush(seller);
    }
}
