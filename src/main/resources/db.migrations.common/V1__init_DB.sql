CREATE TABLE categories
(
    id          VARCHAR(255) NOT NULL,
    tenant_id   VARCHAR(255) NOT NULL,
    created_at  TIMESTAMP WITHOUT TIME ZONE NOT NULL,
    updated_at  TIMESTAMP WITHOUT TIME ZONE,
    deleted     BOOLEAN      NOT NULL,
    name        VARCHAR(255) NOT NULL,
    description VARCHAR(255),
    CONSTRAINT pk_categories PRIMARY KEY (id)
);

CREATE TABLE products
(
    id                VARCHAR(255) NOT NULL,
    tenant_id         VARCHAR(255) NOT NULL,
    created_at        TIMESTAMP WITHOUT TIME ZONE NOT NULL,
    updated_at        TIMESTAMP WITHOUT TIME ZONE,
    deleted           BOOLEAN      NOT NULL,
    name              VARCHAR(255) NOT NULL,
    reference         VARCHAR(255) NOT NULL,
    description       TEXT         NOT NULL,
    "alert threshold" INTEGER      NOT NULL,
    price             DECIMAL      NOT NULL,
    "category id"     VARCHAR(255),
    CONSTRAINT pk_products PRIMARY KEY (id)
);

CREATE TABLE stock_mvts
(
    id         VARCHAR(255) NOT NULL,
    tenant_id  VARCHAR(255) NOT NULL,
    created_at TIMESTAMP WITHOUT TIME ZONE NOT NULL,
    updated_at TIMESTAMP WITHOUT TIME ZONE,
    deleted    BOOLEAN      NOT NULL,
    "type myt" VARCHAR(255) NOT NULL,
    quantity   INTEGER      NOT NULL,
    "date mvt" date         NOT NULL,
    comment    TEXT,
    product_id VARCHAR(255),
    CONSTRAINT pk_stock_mvts PRIMARY KEY (id)
);
