CREATE TABLE checkout_order (
                                id BIGINT NOT NULL AUTO_INCREMENT,
                                user_id BIGINT NOT NULL,
                                status VARCHAR(20) NOT NULL,
                                total_amount BIGINT NOT NULL,
                                discount_amount BIGINT NOT NULL,
                                final_amount BIGINT NOT NULL,
                                completed_at DATETIME(6) NULL,
                                canceled_at DATETIME(6) NULL,
                                created_at DATETIME(6) NOT NULL,
                                updated_at DATETIME(6) NOT NULL,
                                PRIMARY KEY (id),
                                CONSTRAINT fk_checkout_order_user
                                    FOREIGN KEY (user_id) REFERENCES users (id),
                                CONSTRAINT ck_checkout_order_total_amount
                                    CHECK (total_amount >= 0),
                                CONSTRAINT ck_checkout_order_discount_amount
                                    CHECK (discount_amount >= 0),
                                CONSTRAINT ck_checkout_order_final_amount
                                    CHECK (final_amount >= 0)
);

CREATE TABLE seller_order (
                              id BIGINT NOT NULL AUTO_INCREMENT,
                              checkout_order_id BIGINT NOT NULL,
                              seller_id BIGINT NOT NULL,
                              status VARCHAR(20) NOT NULL,
                              total_amount BIGINT NOT NULL,
                              discount_amount BIGINT NOT NULL,
                              final_amount BIGINT NOT NULL,
                              completed_at DATETIME(6) NULL,
                              canceled_at DATETIME(6) NULL,
                              created_at DATETIME(6) NOT NULL,
                              updated_at DATETIME(6) NOT NULL,
                              PRIMARY KEY (id),
                              CONSTRAINT fk_seller_order_checkout_order
                                  FOREIGN KEY (checkout_order_id) REFERENCES checkout_order (id),
                              CONSTRAINT fk_seller_order_seller
                                  FOREIGN KEY (seller_id) REFERENCES users (id),
                              CONSTRAINT ck_seller_order_total_amount
                                  CHECK (total_amount >= 0),
                              CONSTRAINT ck_seller_order_discount_amount
                                  CHECK (discount_amount >= 0),
                              CONSTRAINT ck_seller_order_final_amount
                                  CHECK (final_amount >= 0)
);

CREATE TABLE order_item (
                            id BIGINT NOT NULL AUTO_INCREMENT,
                            seller_order_id BIGINT NOT NULL,
                            product_id BIGINT NOT NULL,
                            product_name_snapshot VARCHAR(100) NOT NULL,
                            product_price_snapshot BIGINT NOT NULL,
                            quantity INT NOT NULL,
                            line_amount BIGINT NOT NULL,
                            created_at DATETIME(6) NOT NULL,
                            updated_at DATETIME(6) NOT NULL,
                            PRIMARY KEY (id),
                            CONSTRAINT fk_order_item_seller_order
                                FOREIGN KEY (seller_order_id) REFERENCES seller_order (id),
                            CONSTRAINT fk_order_item_product
                                FOREIGN KEY (product_id) REFERENCES products (id),
                            CONSTRAINT ck_order_item_quantity
                                CHECK (quantity >= 1),
                            CONSTRAINT ck_order_item_product_price_snapshot
                                CHECK (product_price_snapshot >= 0),
                            CONSTRAINT ck_order_item_line_amount
                                CHECK (line_amount >= 0)
);
