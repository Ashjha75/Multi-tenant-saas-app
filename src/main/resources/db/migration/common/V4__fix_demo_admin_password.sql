-- The bcrypt hash seeded in V3 did not correspond to any known password, so the demo
-- admin could never log in on a fresh install. Reset it to the password documented in
-- docs/postman/MultiTanant_SAAS_API.postman_collection.json (PLATFORM_PASSWORD).
--
-- Password: SecurePassword123
-- Encoder:  BCryptPasswordEncoder (strength 10) -- see config/BeansConfig.java

UPDATE tenants
SET admin_password = '$2a$10$oV75pcPHNBzrgfT7F1sRMueWXYDiAoRQSuwk9Y8B3fYLRCvt.WSfW'
WHERE company_code = 'acme-corp';

UPDATE users
SET password = '$2a$10$oV75pcPHNBzrgfT7F1sRMueWXYDiAoRQSuwk9Y8B3fYLRCvt.WSfW'
WHERE username = 'admin';
