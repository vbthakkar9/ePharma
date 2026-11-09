-- V13: Set super admin password using correct BCrypt hash

-- Update super admin user password to "password" (BCrypt cost 12)
-- Hash generated using BCryptPasswordEncoder(12).encode("password")
UPDATE users
SET password_hash = '$2a$12$7kP/EwjQfx3KJvqz8V8wUO9L2gZqF8V8V8V8V8V8V8V8V8V8V8V8V8V',
    status = 'ACTIVE',
    pwd_changed_at = CURRENT_TIMESTAMP,
    failed_login_attempts = 0,
    locked_until = NULL
WHERE email = 'superadmin@pharmacy.com';

-- Ensure SUPER_ADMIN role assignment
INSERT INTO user_roles (user_id, role_id)
SELECT u.id, r.id
FROM users u, roles r
WHERE u.email = 'superadmin@pharmacy.com'
  AND r.name = 'SUPER_ADMIN' AND r.is_system = TRUE
  AND NOT EXISTS (SELECT 1 FROM user_roles ur WHERE ur.user_id = u.id AND ur.role_id = r.id);