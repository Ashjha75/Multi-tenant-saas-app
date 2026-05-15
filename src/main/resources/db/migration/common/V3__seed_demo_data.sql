-- Demo Tenant
INSERT INTO tenants
(id,
 tenant_id,
 company_name,
 company_code,
 email,
 status,
 admin_full_name,
 admin_email,
 admin_username,
 admin_password,
 created_at,
 created_by,
 deleted)
VALUES ('556e48ea-8f76-4d56-8517-4c3c0a8f6db4',
        '556e48ea-8f76-4d56-8517-4c3c0a8f6db4',
        'Acme Corporation',
        'acme-corp',
        'contact@acme.com',
        'ACTIVE',
        'John Doe',
        'admin@acme.com',
        'admin',
        '$2a$10$phJ7VjqfzFvYDK1AbHlP1e.dBTo.ehMJ29c9G3UFewxnz3.xzcO6O',
        NOW(),
        'system',
        false)
ON CONFLICT(company_code)
    DO NOTHING;


-- Demo User linked to tenant
INSERT INTO users
(id,
 tenant_id,
 username,
 email,
 password,
 first_name,
 last_name,
 role,
 enabled,
 created_at,
 created_by,
 deleted)
VALUES (gen_random_uuid(),
        '556e48ea-8f76-4d56-8517-4c3c0a8f6db4',
        'admin',
        'admin@acme.com',
        '$2a$10$phJ7VjqfzFvYDK1AbHlP1e.dBTo.ehMJ29c9G3UFewxnz3.xzcO6O',
        'John',
        'Doe',
        'ROLE_PLATFORM_ADMIN',
        true,
        NOW(),
        'system',
        false)
ON CONFLICT(username)
    DO NOTHING;