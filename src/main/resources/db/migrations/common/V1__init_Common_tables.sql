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
CREATE TABLE tenants
(
    -- AbstractEntity base columns
    id          VARCHAR(255)                NOT NULL,
    tenant_id   VARCHAR(255)                NOT NULL,
    created_at  TIMESTAMP WITHOUT TIME ZONE NOT NULL,
    updated_at  TIMESTAMP WITHOUT TIME ZONE,
    created_by  VARCHAR(255)                NOT NULL,
    updated_by  VARCHAR(255),
    deleted     BOOLEAN                     NOT NULL DEFAULT FALSE,

    -- Tenant-specific columns  (@Column name must match exactly)
    "company name"    VARCHAR(255) NOT NULL,
    "company code"    VARCHAR(255) NOT NULL,
    email             VARCHAR(255) NOT NULL,
    status            VARCHAR(255) NOT NULL DEFAULT 'PENDING',
    "admin full name" VARCHAR(255) NOT NULL,
    "admin email"     VARCHAR(255) NOT NULL,
    "admin username"  VARCHAR(255) NOT NULL,
    "admin password"  VARCHAR(255) NOT NULL,

    CONSTRAINT pk_tenants PRIMARY KEY (id),
    CONSTRAINT chk_tenant_status CHECK (status IN ('PENDING', 'ACTIVE', 'SUSPENDED', 'INACTIVE'))
);

ALTER TABLE tenants ADD CONSTRAINT uc_tenants_company_code  UNIQUE ("company code");
ALTER TABLE tenants ADD CONSTRAINT uc_tenants_email         UNIQUE (email);
ALTER TABLE tenants ADD CONSTRAINT uc_tenants_admin_email   UNIQUE ("admin email");
ALTER TABLE tenants ADD CONSTRAINT uc_tenants_admin_username UNIQUE ("admin username");

-- ----------------------------------------------------------
-- users
-- Matches: User extends AbstractEntity implements UserDetails
-- FK:      "tenant id" → tenants(id)   (JoinColumn name = "tenant id")
-- Enums:   UserRole → ROLE_PLATFORM_ADMIN | ROLE_COMPANY_ADMIN |
--                     ROLE_ADMINISTRATOR  | ROLE_USER | ROLE_SALES_OPERATOR
-- NOTE:    AbstractEntity already carries tenant_id (audit copy).
--          The ManyToOne FK column is named "tenant id" (with space) per entity.
--          "enabled " has a trailing space to match @Column(name = "enabled ")
-- ----------------------------------------------------------
CREATE TABLE users
(
    -- AbstractEntity base columns
    id          VARCHAR(255)                NOT NULL,
    tenant_id   VARCHAR(255)                NOT NULL,
    created_at  TIMESTAMP WITHOUT TIME ZONE NOT NULL,
    updated_at  TIMESTAMP WITHOUT TIME ZONE,
    created_by  VARCHAR(255)                NOT NULL,
    updated_by  VARCHAR(255),
    deleted     BOOLEAN                     NOT NULL DEFAULT FALSE,

    -- User-specific columns
    "tenant id"  VARCHAR(255),               -- FK to tenants.id  (ManyToOne)
    username     VARCHAR(255) NOT NULL,
    email        VARCHAR(255) NOT NULL,
    password     VARCHAR(255) NOT NULL,
    "first name" VARCHAR(255) NOT NULL,
    "Last name"  VARCHAR(255) NOT NULL,      -- matches @Column(name = "Last name")
    role         VARCHAR(255) NOT NULL,
    "enabled "   BOOLEAN,                    -- trailing space matches entity @Column

    CONSTRAINT pk_users PRIMARY KEY (id),
    CONSTRAINT chk_user_role CHECK (role IN (
        'ROLE_PLATFORM_ADMIN',
        'ROLE_COMPANY_ADMIN',
        'ROLE_ADMINISTRATOR',
        'ROLE_USER',
        'ROLE_SALES_OPERATOR'
    ))
);

ALTER TABLE users ADD CONSTRAINT uc_users_username UNIQUE (username);
ALTER TABLE users ADD CONSTRAINT uc_users_email    UNIQUE (email);

ALTER TABLE users
    ADD CONSTRAINT fk_user_tenant_id
        FOREIGN KEY ("tenant id") REFERENCES tenants (id);