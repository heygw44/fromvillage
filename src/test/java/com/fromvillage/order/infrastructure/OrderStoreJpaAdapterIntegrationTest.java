package com.fromvillage.order.infrastructure;

import com.fromvillage.common.config.JpaAuditingConfig;
import com.fromvillage.order.domain.CheckoutOrder;
import com.fromvillage.order.domain.CheckoutOrderStore;
import com.fromvillage.order.domain.OrderItem;
import com.fromvillage.order.domain.SellerOrder;
import com.fromvillage.order.domain.SellerOrderStore;
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

@DataJpaTest
@ActiveProfiles("test")
@Import({
        TestContainersConfig.class,
        JpaAuditingConfig.class,
        CheckoutOrderStoreJpaAdapter.class,
        SellerOrderStoreJpaAdapter.class
})
class OrderStoreJpaAdapterIntegrationTest {

    @Autowired
    private CheckoutOrderStore checkoutOrderStore;

    @Autowired
    private SellerOrderStore sellerOrderStore;

    @Autowired
    private UserJpaRepository userJpaRepository;

    @Autowired
    private com.fromvillage.product.infrastructure.ProductJpaRepository productJpaRepository;

    @Test
    @DisplayName("체크아웃 주문 그래프를 저장하고 seller order, order item까지 함께 조회할 수 있다")
    void saveAndFindCheckoutOrderGraph() {
        User buyer = createUser("buyer@example.com", "buyer");
        User seller1 = createSeller("seller1@example.com", "seller1");
        User seller2 = createSeller("seller2@example.com", "seller2");

        Product potato = createProduct(seller1, "감자", 12000L);
        Product cabbage = createProduct(seller2, "배추", 8000L);

        CheckoutOrder checkoutOrder = CheckoutOrder.create(
                buyer,
                List.of(
                        SellerOrder.create(seller1, List.of(OrderItem.create(potato, 2))),
                        SellerOrder.create(seller2, List.of(OrderItem.create(cabbage, 1)))
                )
        );

        CheckoutOrder saved = checkoutOrderStore.save(checkoutOrder);

        assertThat(saved.getId()).isNotNull();
        assertThat(saved.getCreatedAt()).isNotNull();
        assertThat(saved.getUpdatedAt()).isNotNull();

        CheckoutOrder found = checkoutOrderStore.findById(saved.getId()).orElseThrow();

        assertThat(found.getSellerOrders()).hasSize(2);
        assertThat(found.getTotalAmount()).isEqualTo(32000L);
        assertThat(found.getSellerOrders())
                .flatExtracting(SellerOrder::getOrderItems)
                .extracting(OrderItem::getProductNameSnapshot)
                .containsExactlyInAnyOrder("감자", "배추");
    }

    @Test
    @DisplayName("체크아웃 주문 id로 판매자 주문 목록을 조회할 수 있다")
    void findAllSellerOrdersByCheckoutOrderId() {
        User buyer = createUser("buyer@example.com", "buyer");
        User seller1 = createSeller("seller1@example.com", "seller1");
        User seller2 = createSeller("seller2@example.com", "seller2");

        Product potato = createProduct(seller1, "감자", 12000L);
        Product cabbage = createProduct(seller2, "배추", 8000L);

        CheckoutOrder saved = checkoutOrderStore.save(
                CheckoutOrder.create(
                        buyer,
                        List.of(
                                SellerOrder.create(seller1, List.of(OrderItem.create(potato, 2))),
                                SellerOrder.create(seller2, List.of(OrderItem.create(cabbage, 1)))
                        )
                )
        );

        List<SellerOrder> sellerOrders = sellerOrderStore.findAllByCheckoutOrderId(saved.getId());

        assertThat(sellerOrders).hasSize(2);
        assertThat(sellerOrders)
                .extracting(order -> order.getSeller().getEmail())
                .containsExactlyInAnyOrder("seller1@example.com", "seller2@example.com");
    }

    @Test
    @DisplayName("판매자 id로 본인 판매자 주문 목록을 조회할 수 있다")
    void findAllSellerOrdersBySellerId() {
        User buyer = createUser("buyer@example.com", "buyer");
        User seller1 = createSeller("seller1@example.com", "seller1");
        User seller2 = createSeller("seller2@example.com", "seller2");

        Product potato = createProduct(seller1, "감자", 12000L);
        Product cabbage = createProduct(seller2, "배추", 8000L);

        checkoutOrderStore.save(
                CheckoutOrder.create(
                        buyer,
                        List.of(
                                SellerOrder.create(seller1, List.of(OrderItem.create(potato, 2))),
                                SellerOrder.create(seller2, List.of(OrderItem.create(cabbage, 1)))
                        )
                )
        );

        List<SellerOrder> sellerOrders = sellerOrderStore.findAllBySellerId(seller1.getId());

        assertThat(sellerOrders).hasSize(1);
        assertThat(sellerOrders.getFirst().getSeller().getId()).isEqualTo(seller1.getId());
        assertThat(sellerOrders.getFirst().getOrderItems()).hasSize(1);
        assertThat(sellerOrders.getFirst().getOrderItems().getFirst().getProductNameSnapshot()).isEqualTo("감자");
    }

    private User createUser(String email, String nickname) {
        return userJpaRepository.saveAndFlush(User.createUser(email, "encoded-password", nickname));
    }

    private User createSeller(String email, String nickname) {
        User seller = User.createUser(email, "encoded-password", nickname);
        seller.approveSeller(LocalDateTime.of(2026, 3, 12, 10, 0));
        return userJpaRepository.saveAndFlush(seller);
    }

    private Product createProduct(User seller, String name, Long price) {
        Product product = Product.create(
                seller,
                name,
                name + " 상품 설명",
                ProductCategory.AGRICULTURE,
                price,
                10,
                "https://cdn.example.com/" + name + ".jpg"
        );
        return productJpaRepository.saveAndFlush(product);
    }
}
