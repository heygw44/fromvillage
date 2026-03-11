CREATE TABLE cart_item (
                           id BIGINT NOT NULL AUTO_INCREMENT,
                           user_id BIGINT NOT NULL,
                           product_id BIGINT NOT NULL,
                           quantity INT NOT NULL,
                           created_at DATETIME(6) NOT NULL,
                           updated_at DATETIME(6) NOT NULL,
                           PRIMARY KEY (id),
                           CONSTRAINT fk_cart_item_user
                               FOREIGN KEY (user_id) REFERENCES users (id),
                           CONSTRAINT fk_cart_item_product
                               FOREIGN KEY (product_id) REFERENCES products (id),
                           CONSTRAINT uk_cart_item_user_product
                               UNIQUE (user_id, product_id),
                           CONSTRAINT ck_cart_item_quantity
                               CHECK (quantity >= 1)
);
