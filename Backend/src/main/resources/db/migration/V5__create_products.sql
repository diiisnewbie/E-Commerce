CREATE TABLE products (
    id          BIGSERIAL PRIMARY KEY,
    sku         VARCHAR(50)   NOT NULL,
    name        VARCHAR(200)  NOT NULL,
    description TEXT,
    price       DECIMAL(19,2) NOT NULL,
    status      VARCHAR(20)   NOT NULL DEFAULT 'ACTIVE',
    category_id BIGINT        NOT NULL,
    version     BIGINT        NOT NULL DEFAULT 0,
    created_at  TIMESTAMPTZ   NOT NULL DEFAULT NOW(),
    updated_at  TIMESTAMPTZ   NOT NULL DEFAULT NOW(),
    CONSTRAINT uk_products_sku UNIQUE (sku),
    CONSTRAINT ck_products_sku_uppercase CHECK (sku = upper(sku)),
    CONSTRAINT ck_products_status CHECK (status IN ('ACTIVE', 'INACTIVE')),
    CONSTRAINT ck_products_price_positive CHECK (price > 0),
    CONSTRAINT fk_products_category FOREIGN KEY (category_id) REFERENCES categories (id)
);

CREATE INDEX idx_products_category_id ON products (category_id);
CREATE INDEX idx_products_active_created
    ON products (created_at DESC, id DESC) WHERE status = 'ACTIVE';
