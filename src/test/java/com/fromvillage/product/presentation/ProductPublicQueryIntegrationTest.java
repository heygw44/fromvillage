package com.fromvillage.product.presentation;

import com.fromvillage.product.domain.Product;
import com.fromvillage.product.domain.ProductCategory;
import com.fromvillage.support.TestContainersConfig;
import com.fromvillage.user.domain.User;
import com.fromvillage.user.infrastructure.UserJpaRepository;
import com.fromvillage.cart.infrastructure.CartJpaRepository;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.context.annotation.Import;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.test.web.servlet.MockMvc;

import java.sql.Timestamp;
import java.time.LocalDateTime;

import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

@SpringBootTest
@AutoConfigureMockMvc
@ActiveProfiles("test")
@Import(TestContainersConfig.class)
class ProductPublicQueryIntegrationTest {

    @Autowired
    private MockMvc mockMvc;

    @Autowired
    private UserJpaRepository userJpaRepository;

    @Autowired
    private JdbcTemplate jdbcTemplate;

    @Autowired
    private com.fromvillage.product.infrastructure.ProductJpaRepository productJpaRepository;

    @Autowired
    private CartJpaRepository cartJpaRepository;

    @BeforeEach
    void setUp() {
        cartJpaRepository.deleteAll();
        productJpaRepository.deleteAll();
        userJpaRepository.deleteAll();
    }

    @Test
    @DisplayName("공개 상품 목록은 인증 없이 조회할 수 있고 필터와 정렬을 적용한다")
    void getProductsReturnsPublicPage() throws Exception {
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
        productJpaRepository.saveAndFlush(Product.create(
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

        mockMvc.perform(get("/api/v1/products")
                        .param("keyword", "감자")
                        .param("category", "AGRICULTURE")
                        .param("sort", "price,asc")
                        .param("page", "0")
                        .param("size", "10"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.success").value(true))
                .andExpect(jsonPath("$.data.content.length()").value(2))
                .andExpect(jsonPath("$.data.content[0].name").value("유기농 감자 5kg"))
                .andExpect(jsonPath("$.data.content[0].description").doesNotExist())
                .andExpect(jsonPath("$.data.content[0].price").value(22000))
                .andExpect(jsonPath("$.data.content[1].name").value("유기농 감자 10kg"))
                .andExpect(jsonPath("$.data.page").value(0))
                .andExpect(jsonPath("$.data.size").value(10))
                .andExpect(jsonPath("$.data.totalElements").value(2))
                .andExpect(jsonPath("$.data.totalPages").value(1))
                .andExpect(jsonPath("$.data.hasNext").value(false));
    }

    @Test
    @DisplayName("공개 상품 목록은 soft delete 상품을 제외한다")
    void getProductsExcludesDeletedProducts() throws Exception {
        User seller = createSeller("seller@example.com");
        Product activeProduct = productJpaRepository.saveAndFlush(Product.create(
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
        markDeleted(deletedProduct.getId());

        mockMvc.perform(get("/api/v1/products")
                        .param("sort", "price,asc"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.data.content.length()").value(1))
                .andExpect(jsonPath("$.data.content[0].productId").value(activeProduct.getId()))
                .andExpect(jsonPath("$.data.content[0].name").value("유기농 감자 5kg"));
    }

    @Test
    @DisplayName("공개 상품 상세는 description을 포함해 조회한다")
    void getProductReturnsPublicDetail() throws Exception {
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

        mockMvc.perform(get("/api/v1/products/{productId}", product.getId()))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.success").value(true))
                .andExpect(jsonPath("$.data.productId").value(product.getId()))
                .andExpect(jsonPath("$.data.name").value("완도 활전복 1kg"))
                .andExpect(jsonPath("$.data.description").value("완도 산지 직송"))
                .andExpect(jsonPath("$.data.category").value("FISHERY"))
                .andExpect(jsonPath("$.data.price").value(45000))
                .andExpect(jsonPath("$.data.stockQuantity").value(3))
                .andExpect(jsonPath("$.data.status").value("ON_SALE"))
                .andExpect(jsonPath("$.data.imageUrl").value("https://cdn.example.com/abalone.jpg"));
    }

    @Test
    @DisplayName("존재하지 않는 상품 상세 조회는 PRODUCT_NOT_FOUND를 반환한다")
    void getProductReturnsNotFoundWhenMissing() throws Exception {
        mockMvc.perform(get("/api/v1/products/{productId}", 9999L))
                .andExpect(status().isNotFound())
                .andExpect(jsonPath("$.code").value("PRODUCT_NOT_FOUND"));
    }

    @Test
    @DisplayName("soft delete 상품 상세 조회는 PRODUCT_NOT_FOUND를 반환한다")
    void getProductReturnsNotFoundWhenDeleted() throws Exception {
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

        mockMvc.perform(get("/api/v1/products/{productId}", product.getId()))
                .andExpect(status().isNotFound())
                .andExpect(jsonPath("$.code").value("PRODUCT_NOT_FOUND"));
    }

    @Test
    @DisplayName("허용되지 않은 sort 값은 VALIDATION_ERROR를 반환한다")
    void getProductsRejectsUnsupportedSort() throws Exception {
        mockMvc.perform(get("/api/v1/products")
                        .param("sort", "name,asc"))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.code").value("VALIDATION_ERROR"));
    }

    @Test
    @DisplayName("허용되지 않은 category 값은 VALIDATION_ERROR를 반환한다")
    void getProductsRejectsUnsupportedCategory() throws Exception {
        mockMvc.perform(get("/api/v1/products")
                        .param("category", "FRUIT"))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.code").value("VALIDATION_ERROR"));
    }

    @Test
    @DisplayName("size가 100을 초과하면 VALIDATION_ERROR를 반환한다")
    void getProductsRejectsTooLargeSize() throws Exception {
        mockMvc.perform(get("/api/v1/products")
                        .param("size", "101"))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.code").value("VALIDATION_ERROR"));
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
