CREATE TABLE product_inventory (
    product_id BIGINT PRIMARY KEY,
    quantity   BIGINT      NOT NULL DEFAULT 0,
    updated_at TIMESTAMPTZ NOT NULL DEFAULT NOW(),
    CONSTRAINT ck_inventory_quantity_non_negative CHECK (quantity >= 0),
    CONSTRAINT fk_inventory_product FOREIGN KEY (product_id)
        REFERENCES products (id) ON DELETE CASCADE
);
