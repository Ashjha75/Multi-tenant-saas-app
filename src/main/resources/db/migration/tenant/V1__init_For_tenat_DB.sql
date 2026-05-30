-- ============================================================
-- TENANT SCHEMA: tables that live inside each tenant's schema
-- categories, products, stock_mvts
-- ============================================================

-- ----------------------------------------------------------
-- categories
-- Matches: Category extends AbstractEntity
-- ----------------------------------------------------------
CREATE TABLE IF NOT EXISTS categories
(
    -- AbstractEntity base columns
    id          VARCHAR(255) NOT NULL,
    tenant_id   VARCHAR(255) NOT NULL,
    created_at  TIMESTAMP WITHOUT TIME ZONE NOT NULL,
    updated_at  TIMESTAMP WITHOUT TIME ZONE,
    created_by  VARCHAR(255) NOT NULL,
    updated_by  VARCHAR(255),
    deleted     BOOLEAN      NOT NULL DEFAULT FALSE,

    -- Category-specific columns
    name        VARCHAR(255) NOT NULL,
    description VARCHAR(255),

    CONSTRAINT pk_categories PRIMARY KEY (id)
);

-- ----------------------------------------------------------
-- products
-- Matches: Product extends AbstractEntity
-- FK:      category_id → categories(id)
-- ----------------------------------------------------------
CREATE TABLE IF NOT EXISTS products
(
    -- AbstractEntity base columns
    id              VARCHAR(255)   NOT NULL,
    tenant_id       VARCHAR(255)   NOT NULL,
    created_at      TIMESTAMP WITHOUT TIME ZONE NOT NULL,
    updated_at      TIMESTAMP WITHOUT TIME ZONE,
    created_by      VARCHAR(255)   NOT NULL,
    updated_by      VARCHAR(255),
    deleted         BOOLEAN        NOT NULL DEFAULT FALSE,

    -- Product-specific columns
    name            VARCHAR(255)   NOT NULL,
    reference       VARCHAR(255)   NOT NULL,
    description     TEXT           NOT NULL,
    alert_threshold INTEGER        NOT NULL,
    price           DECIMAL(19, 2) NOT NULL,
    category_id     VARCHAR(255),

    CONSTRAINT pk_products PRIMARY KEY (id)
);

ALTER TABLE products
    ADD CONSTRAINT uc_products_reference UNIQUE (reference);

ALTER TABLE products
    ADD CONSTRAINT fk_products_on_category
        FOREIGN KEY (category_id) REFERENCES categories (id);

-- ----------------------------------------------------------
-- stock_mvts
-- Matches: StockMvt extends AbstractEntity
-- FK:      product_id → products(id)
-- Enums:   TypeMvt → IN | OUT  (stored as VARCHAR)
-- ----------------------------------------------------------
CREATE TABLE IF NOT EXISTS stock_mvts
(
    -- AbstractEntity base columns
    id         VARCHAR(255) NOT NULL,
    tenant_id  VARCHAR(255) NOT NULL,
    created_at TIMESTAMP WITHOUT TIME ZONE NOT NULL,
    updated_at TIMESTAMP WITHOUT TIME ZONE,
    created_by VARCHAR(255) NOT NULL,
    updated_by VARCHAR(255),
    deleted    BOOLEAN      NOT NULL DEFAULT FALSE,

    -- StockMvt-specific columns
    type_mvt   VARCHAR(255) NOT NULL,
    quantity   INTEGER      NOT NULL,
    date_mvt   DATE         NOT NULL,
    comment    TEXT,
    product_id VARCHAR(255),

    CONSTRAINT pk_stock_mvts PRIMARY KEY (id),
    CONSTRAINT chk_type_mvt CHECK (type_mvt IN ('IN', 'OUT'))
);

ALTER TABLE stock_mvts
    ADD CONSTRAINT fk_stock_mvts_on_product
        FOREIGN KEY (product_id) REFERENCES products (id);
