package com.fromvillage.product.infrastructure;

import com.fromvillage.common.config.JpaAuditingConfig;
import com.fromvillage.product.domain.Product;
import com.fromvillage.product.domain.ProductCategory;
import com.fromvillage.product.domain.ProductPageRequest;
import com.fromvillage.product.domain.ProductPublicQueryPort;
import com.fromvillage.product.domain.ProductPublicQueryCondition;
import com.fromvillage.product.domain.ProductPublicSort;
import com.fromvillage.support.TestContainersConfig;
import com.fromvillage.user.domain.User;
import com.fromvillage.user.infrastructure.UserJpaRepository;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.orm.jpa.DataJpaTest;
import org.springframework.context.annotation.Import;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.test.context.ActiveProfiles;

import java.sql.Timestamp;
import java.time.LocalDateTime;

import static org.assertj.core.api.Assertions.assertThat;

@DataJpaTest
@ActiveProfiles("test")
@Import({TestContainersConfig.class, JpaAuditingConfig.class, ProductPublicQueryJpaAdapter.class})
class ProductPublicQueryJpaAdapterTest {

    @Autowired
    private ProductPublicQueryPort productPublicQueryPort;

    @Autowired
    private ProductJpaRepository productJpaRepository;

    @Autowired
    private UserJpaRepository userJpaRepository;

    @Autowired
    private JdbcTemplate jdbcTemplate;

    @Test
    @DisplayName("공개 상품 목록 조회는 키워드, 카테고리, soft delete 제외를 함께 적용한다")
    void findPublicProductsAppliesFilters() {
        User seller = createSeller("seller@example.com");
        productJpaRepository.saveAndFlush(Product.create(
                seller,
                "유기농 감자 5kg",
                "해남 햇감자",
                ProductCategory.AGRICULTURE,
                22000L,
                10,
                "https://cdn.example.com/potato.jpg"
        ));
        Product deletedProduct = productJpaRepository.saveAndFlush(Product.create(
                seller,
                "유기농 감자 10kg",
                "해남 햇감자",
                ProductCategory.AGRICULTURE,
                39000L,
                5,
                "https://cdn.example.com/potato-premium.jpg"
        ));
        productJpaRepository.saveAndFlush(Product.create(
                seller,
                "완도 활전복 1kg",
                "완도 산지 직송",
                ProductCategory.FISHERY,
                45000L,
                3,
                "https://cdn.example.com/abalone.jpg"
        ));
        markDeleted(deletedProduct.getId());

        var result = productPublicQueryPort.findPublicProducts(
                new ProductPublicQueryCondition("감자", ProductCategory.AGRICULTURE),
                new ProductPageRequest(0, 10, ProductPublicSort.PRICE_ASC)
        );

        assertThat(result.content()).hasSize(1);
        assertThat(result.content().get(0).getName()).isEqualTo("유기농 감자 5kg");
        assertThat(result.totalElements()).isEqualTo(1);
    }

    @Test
    @DisplayName("공개 상품 상세 조회는 soft delete 상품을 반환하지 않는다")
    void findPublicProductByIdExcludesDeletedProduct() {
        User seller = createSeller("seller@example.com");
        Product product = productJpaRepository.saveAndFlush(Product.create(
                seller,
                "완도 활전복 1kg",
                "완도 산지 직송",
                ProductCategory.FISHERY,
                45000L,
                3,
                "https://cdn.example.com/abalone.jpg"
        ));
        markDeleted(product.getId());

        assertThat(productPublicQueryPort.findPublicProductById(product.getId())).isEmpty();
    }

    private User createSeller(String email) {
        User seller = User.createUser(email, "encoded-password", "seller");
        seller.approveSeller(LocalDateTime.of(2026, 3, 10, 0, 0));
        return userJpaRepository.saveAndFlush(seller);
    }

    private void markDeleted(Long productId) {
        jdbcTemplate.update(
                "update products set deleted_at = ? where id = ?",
                Timestamp.valueOf(LocalDateTime.of(2026, 3, 10, 12, 0)),
                productId
        );
    }
}
