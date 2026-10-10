-- ============================================================
-- Business tables: categories, products, stock_mvts
--
-- These are the tables defined in db/migration/tenant/V1__init_For_tenat_DB.sql,
-- which is a Flyway location that is never executed: spring.flyway.locations points
-- only at db/migration/common, and schema-per-tenant provisioning is not implemented
-- (see docs/tenant-toggle.md). The result was that every /api/v1/categories,
-- /products and /stock-mvts request failed with: relation "categories" does not exist.
--
-- In `filter` mode -- the implemented, documented isolation mode -- all tenants share
-- these tables and are separated by the tenant_id column via the Hibernate
-- "tenantFilter" (see AbstractEntity). So they belong in the common/public schema.
--
-- The tenant/ location is kept as the template for a future schema-per-tenant mode.
-- ============================================================

CREATE TABLE IF NOT EXISTS categories
(
    id          VARCHAR(255) NOT NULL,
    tenant_id   VARCHAR(255) NOT NULL,
    created_at  TIMESTAMP WITHOUT TIME ZONE NOT NULL,
    updated_at  TIMESTAMP WITHOUT TIME ZONE,
    created_by  VARCHAR(255) NOT NULL,
    updated_by  VARCHAR(255),
    deleted     BOOLEAN      NOT NULL DEFAULT FALSE,

    name        VARCHAR(255) NOT NULL,
    description VARCHAR(255),

    CONSTRAINT pk_categories PRIMARY KEY (id)
);

CREATE TABLE IF NOT EXISTS products
(
    id              VARCHAR(255)   NOT NULL,
    tenant_id       VARCHAR(255)   NOT NULL,
    created_at      TIMESTAMP WITHOUT TIME ZONE NOT NULL,
    updated_at      TIMESTAMP WITHOUT TIME ZONE,
    created_by      VARCHAR(255)   NOT NULL,
    updated_by      VARCHAR(255),
    deleted         BOOLEAN        NOT NULL DEFAULT FALSE,

    name            VARCHAR(255)   NOT NULL,
    reference       VARCHAR(255)   NOT NULL,
    description     TEXT           NOT NULL,
    alert_threshold INTEGER        NOT NULL,
    price           DECIMAL(19, 2) NOT NULL,
    category_id     VARCHAR(255),

    CONSTRAINT pk_products PRIMARY KEY (id),
    CONSTRAINT uc_products_reference UNIQUE (reference),
    CONSTRAINT fk_products_on_category
        FOREIGN KEY (category_id) REFERENCES categories (id)
);

CREATE TABLE IF NOT EXISTS stock_mvts
(
    id         VARCHAR(255) NOT NULL,
    tenant_id  VARCHAR(255) NOT NULL,
    created_at TIMESTAMP WITHOUT TIME ZONE NOT NULL,
    updated_at TIMESTAMP WITHOUT TIME ZONE,
    created_by VARCHAR(255) NOT NULL,
    updated_by VARCHAR(255),
    deleted    BOOLEAN      NOT NULL DEFAULT FALSE,

    type_mvt   VARCHAR(255) NOT NULL,
    quantity   INTEGER      NOT NULL,
    date_mvt   DATE         NOT NULL,
    comment    TEXT,
    product_id VARCHAR(255),

    CONSTRAINT pk_stock_mvts PRIMARY KEY (id),
    CONSTRAINT chk_type_mvt CHECK (type_mvt IN ('IN', 'OUT')),
    CONSTRAINT fk_stock_mvts_on_product
        FOREIGN KEY (product_id) REFERENCES products (id)
);

-- Every query in filter mode carries "WHERE tenant_id = ?", so index it.
CREATE INDEX IF NOT EXISTS idx_categories_tenant_id ON categories (tenant_id);
CREATE INDEX IF NOT EXISTS idx_products_tenant_id   ON products (tenant_id);
CREATE INDEX IF NOT EXISTS idx_stock_mvts_tenant_id ON stock_mvts (tenant_id);
