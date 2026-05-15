ALTER TABLE tenants
    ADD CONSTRAINT chk_tenants_status CHECK (status IN ('PENDING', 'ACTIVE', 'SUSPENDED', 'INACTIVE'));

ALTER TABLE users
    ADD CONSTRAINT chk_users_role CHECK (role IN (
        'ROLE_PLATFORM_ADMIN',
        'ROLE_COMPANY_ADMIN',
        'ROLE_ADMINISTRATOR',
        'ROLE_USER',
        'ROLE_SALES_OPERATOR'
    ));

ALTER TABLE stock_mvts
    ADD CONSTRAINT chk_stock_mvts_type_mvt CHECK (type_mvt IN ('IN', 'OUT'));

