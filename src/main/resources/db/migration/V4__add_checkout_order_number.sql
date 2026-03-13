ALTER TABLE checkout_order
    ADD COLUMN order_number VARCHAR(40) NULL AFTER id;

UPDATE checkout_order
SET order_number = CONCAT('ORD-', REPLACE(UPPER(UUID()), '-', ''))
WHERE order_number IS NULL;

ALTER TABLE checkout_order
    MODIFY COLUMN order_number VARCHAR(40) NOT NULL,
    ADD CONSTRAINT uk_checkout_order_order_number UNIQUE (order_number);
