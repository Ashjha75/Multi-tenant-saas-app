-- ============================================================
-- COMMON SCHEMA: only cross-tenant tables live here
-- tenants  → stores every registered tenant (company)
-- users    → platform-level users linked to a tenant
-- ============================================================

-- ----------------------------------------------------------
-- tenants
-- Matches: Tenant extends AbstractEntity
-- Enums:   TenantStatus → PENDING | ACTIVE | SUSPENDED | INACTIVE
-- ----------------------------------------------------------
CREATE TABLE IF NOT EXISTS tenants
(
    -- AbstractEntity base columns
    id              VARCHAR(255) NOT NULL,
    tenant_id       VARCHAR(255) NOT NULL,
    created_at      TIMESTAMP WITHOUT TIME ZONE NOT NULL,
    updated_at      TIMESTAMP WITHOUT TIME ZONE,
    created_by      VARCHAR(255) NOT NULL,
    updated_by      VARCHAR(255),
    deleted         BOOLEAN      NOT NULL DEFAULT FALSE,

    -- Tenant-specific columns
    company_name    VARCHAR(255) NOT NULL,
    company_code    VARCHAR(255) NOT NULL,
    email           VARCHAR(255) NOT NULL,
    status          VARCHAR(255) NOT NULL DEFAULT 'PENDING',
    admin_full_name VARCHAR(255) NOT NULL,
    admin_email     VARCHAR(255) NOT NULL,
    admin_username  VARCHAR(255) NOT NULL,
    admin_password  VARCHAR(255) NOT NULL,

    CONSTRAINT pk_tenants PRIMARY KEY (id),
    CONSTRAINT chk_tenant_status CHECK (status IN ('PENDING', 'ACTIVE', 'SUSPENDED', 'INACTIVE'))
);

ALTER TABLE tenants
    ADD CONSTRAINT uc_tenants_company_code UNIQUE (company_code);
ALTER TABLE tenants
    ADD CONSTRAINT uc_tenants_email UNIQUE (email);
ALTER TABLE tenants
    ADD CONSTRAINT uc_tenants_admin_email UNIQUE (admin_email);
ALTER TABLE tenants
    ADD CONSTRAINT uc_tenants_admin_username UNIQUE (admin_username);

-- ----------------------------------------------------------
-- users
-- Matches: User extends AbstractEntity implements UserDetails
-- FK:      "tenant id" → tenants(id)   (JoinColumn name = "tenant id")
-- Enums:   UserRole → ROLE_PLATFORM_ADMIN | ROLE_COMPANY_ADMIN |
--                     ROLE_ADMINISTRATOR  | ROLE_USER | ROLE_SALES_OPERATOR
-- ----------------------------------------------------------
CREATE TABLE IF NOT EXISTS users
(
    -- AbstractEntity base columns
    id          VARCHAR(255) NOT NULL,
    tenant_id   VARCHAR(255) NOT NULL,
    created_at  TIMESTAMP WITHOUT TIME ZONE NOT NULL,
    updated_at  TIMESTAMP WITHOUT TIME ZONE,
    created_by  VARCHAR(255) NOT NULL,
    updated_by  VARCHAR(255),
    deleted     BOOLEAN      NOT NULL DEFAULT FALSE,

    -- User-specific columns
    "tenant id" VARCHAR(255), -- FK to tenants.id  (ManyToOne JoinColumn)
    username    VARCHAR(255) NOT NULL,
    email       VARCHAR(255) NOT NULL,
    password    VARCHAR(255) NOT NULL,
    first_name  VARCHAR(255) NOT NULL,
    last_name   VARCHAR(255) NOT NULL,
    role        VARCHAR(255) NOT NULL,
    enabled     BOOLEAN,

    CONSTRAINT pk_users PRIMARY KEY (id),
    CONSTRAINT chk_user_role CHECK (role IN (
                                             'ROLE_PLATFORM_ADMIN',
                                             'ROLE_COMPANY_ADMIN',
                                             'ROLE_ADMINISTRATOR',
                                             'ROLE_USER',
                                             'ROLE_SALES_OPERATOR'
        ))
);

ALTER TABLE users
    ADD CONSTRAINT uc_users_username UNIQUE (username);
ALTER TABLE users
    ADD CONSTRAINT uc_users_email UNIQUE (email);

ALTER TABLE users
    ADD CONSTRAINT fk_user_tenant_id
        FOREIGN KEY ("tenant id") REFERENCES tenants (id);