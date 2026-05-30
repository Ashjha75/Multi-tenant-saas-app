DO $$
BEGIN IF NOT EXISTS (SELECT 1 FROM pg_constraint WHERE conname = 'chk_tenants_status') THEN
ALTER TABLE tenants
    ADD CONSTRAINT chk_tenants_status CHECK (status IN ('PENDING', 'ACTIVE', 'SUSPENDED', 'INACTIVE'));
END IF;
END $$;

DO $$
BEGIN IF NOT EXISTS (SELECT 1 FROM pg_constraint WHERE conname = 'chk_users_role') THEN
ALTER TABLE users
    ADD CONSTRAINT chk_users_role CHECK (role IN (
                                                  'ROLE_PLATFORM_ADMIN',
                                                  'ROLE_COMPANY_ADMIN',
                                                  'ROLE_ADMINISTRATOR',
                                                  'ROLE_USER',
                                                  'ROLE_SALES_OPERATOR'
        ));
END IF;
END $$;
