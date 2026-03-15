CREATE TABLE issued_coupon (
                               id BIGINT NOT NULL AUTO_INCREMENT,
                               coupon_policy_id BIGINT NOT NULL,
                               user_id BIGINT NOT NULL,
                               status VARCHAR(20) NOT NULL,
                               issued_at DATETIME(6) NOT NULL,
                               used_at DATETIME(6) NULL,
                               created_at DATETIME(6) NOT NULL,
                               updated_at DATETIME(6) NOT NULL,
                               CONSTRAINT pk_issued_coupon PRIMARY KEY (id),
                               CONSTRAINT fk_issued_coupon_policy
                                   FOREIGN KEY (coupon_policy_id) REFERENCES coupon_policy (id),
                               CONSTRAINT fk_issued_coupon_user
                                   FOREIGN KEY (user_id) REFERENCES users (id),
                               CONSTRAINT uk_issued_coupon_policy_user
                                   UNIQUE (coupon_policy_id, user_id)
);
