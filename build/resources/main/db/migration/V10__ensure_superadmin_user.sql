-- V10: Ensure super admin user exists (fix for V9 potential issues)

-- Ensure super admin tenant exists
INSERT INTO tenants (tenant_code, pharmacy_name, email, status)
VALUES ('SUPER_ADMIN', 'Super Admin Tenant', 'superadmin@pharmacy.com', 'ACTIVE')
ON CONFLICT (tenant_code) DO UPDATE SET
    pharmacy_name = EXCLUDED.pharmacy_name,
    email = EXCLUDED.email,
    status = EXCLUDED.status;

-- Ensure super admin user exists
-- Password: "password" (BCrypt cost 12)
INSERT INTO users (tenant_id, first_name, last_name, email, mobile, password_hash, status, pwd_changed_at)
SELECT t.id, 'Super', 'Admin', 'superadmin@pharmacy.com', '+1234567890',
       '$2a$12$LQv3c1yqBWVHxkd0LHAkCOYz6TtxMQJqhN8/LewdBPj/RK.PZvO.S', 'ACTIVE', CURRENT_TIMESTAMP
FROM tenants t
WHERE t.tenant_code = 'SUPER_ADMIN'
  AND NOT EXISTS (SELECT 1 FROM users WHERE email = 'superadmin@pharmacy.com');

-- Ensure SUPER_ADMIN role assignment
INSERT INTO user_roles (user_id, role_id)
SELECT u.id, r.id
FROM users u, roles r
WHERE u.email = 'superadmin@pharmacy.com'
  AND r.name = 'SUPER_ADMIN' AND r.is_system = TRUE
  AND NOT EXISTS (SELECT 1 FROM user_roles ur WHERE ur.user_id = u.id AND ur.role_id = r.id);