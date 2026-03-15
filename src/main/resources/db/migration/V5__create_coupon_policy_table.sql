CREATE TABLE coupon_policy (
    id BIGINT NOT NULL AUTO_INCREMENT,
    name VARCHAR(255) NOT NULL,
    discount_amount BIGINT NOT NULL,
    minimum_order_amount BIGINT NOT NULL,
    total_quantity INT NOT NULL,
    issued_quantity INT NOT NULL,
    started_at DATETIME(6) NOT NULL,
    ended_at DATETIME(6) NOT NULL,
    status VARCHAR(20) NOT NULL,
    created_by BIGINT NOT NULL,
    created_at DATETIME(6) NOT NULL,
    updated_at DATETIME(6) NOT NULL,
    PRIMARY KEY (id),
    CONSTRAINT fk_coupon_policy_created_by
        FOREIGN KEY (created_by) REFERENCES users (id),
    CONSTRAINT ck_coupon_policy_discount_amount
        CHECK (discount_amount > 0),
    CONSTRAINT ck_coupon_policy_minimum_order_amount
        CHECK (minimum_order_amount >= 0),
    CONSTRAINT ck_coupon_policy_total_quantity
        CHECK (total_quantity >= 1),
    CONSTRAINT ck_coupon_policy_issued_quantity
        CHECK (issued_quantity >= 0)
);
