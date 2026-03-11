package com.fromvillage.cart.infrastructure;

import com.fromvillage.cart.domain.CartItem;
import com.fromvillage.cart.domain.CartStore;
import com.fromvillage.common.config.JpaAuditingConfig;
import com.fromvillage.product.domain.Product;
import com.fromvillage.product.domain.ProductCategory;
import com.fromvillage.support.TestContainersConfig;
import com.fromvillage.user.domain.User;
import com.fromvillage.user.infrastructure.UserJpaRepository;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.orm.jpa.DataJpaTest;
import org.springframework.context.annotation.Import;
import org.springframework.test.context.ActiveProfiles;

import java.time.LocalDateTime;
import java.util.List;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

@DataJpaTest
@ActiveProfiles("test")
@Import({TestContainersConfig.class, JpaAuditingConfig.class, CartStoreJpaAdapter.class})
class CartStoreJpaAdapterTest {

    @Autowired
    private CartStore cartStore;

    @Autowired
    private UserJpaRepository userJpaRepository;

    @Autowired
    private com.fromvillage.product.infrastructure.ProductJpaRepository productJpaRepository;

    @Test
    @DisplayName("장바구니 항목을 저장하고 사용자와 상품으로 조회할 수 있다")
    void saveAndFindByUserIdAndProductId() {
        User cartUser = createUser("user@example.com", "buyer");
        User seller = createSeller("seller@example.com", "seller");
        Product product = createProduct(seller, "감자");

        CartItem savedCartItem = cartStore.save(CartItem.create(cartUser, product, 2));

        assertThat(savedCartItem.getId()).isNotNull();
        assertThat(cartStore.findByUserIdAndProductId(cartUser.getId(), product.getId()))
                .isPresent()
                .get()
                .extracting(CartItem::getQuantity)
                .isEqualTo(2);
    }

    @Test
    @DisplayName("같은 사용자가 같은 상품을 중복 저장하면 unique 제약으로 실패한다")
    void saveRejectsDuplicateUserProduct() {
        User cartUser = createUser("user@example.com", "buyer");
        User seller = createSeller("seller@example.com", "seller");
        Product product = createProduct(seller, "감자");

        cartStore.save(CartItem.create(cartUser, product, 1));

        assertThatThrownBy(() -> cartStore.save(CartItem.create(cartUser, product, 3)))
                .isInstanceOf(org.springframework.dao.DataIntegrityViolationException.class);
    }

    @Test
    @DisplayName("사용자 기준으로 장바구니 목록을 조회할 수 있다")
    void findAllByUserId() {
        User cartUser = createUser("user1@example.com", "buyer1");
        User otherUser = createUser("user2@example.com", "buyer2");
        User seller = createSeller("seller@example.com", "seller");

        Product potato = createProduct(seller, "감자");
        Product cabbage = createProduct(seller, "배추");
        Product anchovy = createProduct(seller, "멸치");

        cartStore.save(CartItem.create(cartUser, potato, 1));
        cartStore.save(CartItem.create(cartUser, cabbage, 2));
        cartStore.save(CartItem.create(otherUser, anchovy, 3));

        List<CartItem> cartItems = cartStore.findAllByUserId(cartUser.getId());

        assertThat(cartItems).hasSize(2);
        assertThat(cartItems)
                .extracting(item -> item.getProduct().getName())
                .containsExactlyInAnyOrder("감자", "배추");
    }

    @Test
    @DisplayName("soft delete된 상품은 active 장바구니 목록에서 제외된다")
    void findAllActiveByUserIdExcludesDeletedProducts() {
        User cartUser = createUser("user@example.com", "buyer");
        User seller = createSeller("seller@example.com", "seller");

        Product activeProduct = createProduct(seller, "감자");
        Product deletedProduct = createProduct(seller, "배추");
        deletedProduct.softDelete(LocalDateTime.of(2026, 3, 11, 12, 0));
        productJpaRepository.saveAndFlush(deletedProduct);

        cartStore.save(CartItem.create(cartUser, activeProduct, 1));
        cartStore.save(CartItem.create(cartUser, deletedProduct, 2));

        List<CartItem> cartItems = cartStore.findAllActiveByUserId(cartUser.getId());

        assertThat(cartItems).hasSize(1);
        assertThat(cartItems.getFirst().getProduct().getName()).isEqualTo("감자");
    }

    private User createUser(String email, String nickname) {
        return userJpaRepository.saveAndFlush(User.createUser(email, "encoded-password", nickname));
    }

    private User createSeller(String email, String nickname) {
        User seller = User.createUser(email, "encoded-password", nickname);
        seller.approveSeller(LocalDateTime.of(2026, 3, 11, 10, 0));
        return userJpaRepository.saveAndFlush(seller);
    }

    private Product createProduct(User seller, String name) {
        Product product = Product.create(
                seller,
                name,
                name + " 상품 설명",
                ProductCategory.AGRICULTURE,
                10000L,
                10,
                "https://cdn.example.com/" + name + ".jpg"
        );
        return productJpaRepository.saveAndFlush(product);
    }
}
