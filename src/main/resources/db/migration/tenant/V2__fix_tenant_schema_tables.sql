ALTER TABLE categories
    ADD COLUMN created_by VARCHAR(255) NOT NULL DEFAULT 'system';

ALTER TABLE categories
    ADD COLUMN updated_by VARCHAR(255);

ALTER TABLE categories
    ALTER COLUMN created_by DROP DEFAULT;

ALTER TABLE products
    ADD COLUMN created_by VARCHAR(255) NOT NULL DEFAULT 'system';

ALTER TABLE products
    ADD COLUMN updated_by VARCHAR(255);

ALTER TABLE products
    ALTER COLUMN created_by DROP DEFAULT;

ALTER TABLE stock_mvts
    ADD COLUMN created_by VARCHAR(255) NOT NULL DEFAULT 'system';

ALTER TABLE stock_mvts
    ADD COLUMN updated_by VARCHAR(255);

ALTER TABLE stock_mvts
    ALTER COLUMN created_by DROP DEFAULT;

ALTER TABLE products
    ADD CONSTRAINT uc_products_reference UNIQUE (reference);

ALTER TABLE products
    ADD CONSTRAINT fk_products_on_category FOREIGN KEY (category_id) REFERENCES categories (id);

ALTER TABLE stock_mvts
    ADD CONSTRAINT fk_stock_mvts_on_product FOREIGN KEY (product_id) REFERENCES products (id);

ALTER TABLE stock_mvts
    ADD CONSTRAINT chk_stock_mvts_type_mvt CHECK (type_mvt IN ('IN', 'OUT'));

