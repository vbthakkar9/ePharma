-- V5: Create role_permissions junction table and system role permission matrix

CREATE TABLE role_permissions (
    id UUID PRIMARY KEY DEFAULT gen_random_uuid(),
    role_id UUID NOT NULL REFERENCES roles(id) ON DELETE CASCADE,
    permission_id UUID NOT NULL REFERENCES permissions(id) ON DELETE CASCADE,
    assigned_at TIMESTAMPTZ NOT NULL DEFAULT CURRENT_TIMESTAMP,
    assigned_by UUID REFERENCES users(id),
    created_at TIMESTAMPTZ NOT NULL DEFAULT CURRENT_TIMESTAMP,
    updated_at TIMESTAMPTZ NOT NULL DEFAULT CURRENT_TIMESTAMP,
    version BIGINT NOT NULL DEFAULT 0,
    UNIQUE (role_id, permission_id)
);

CREATE INDEX idx_role_permissions_role_id ON role_permissions(role_id);
CREATE INDEX idx_role_permissions_permission_id ON role_permissions(permission_id);

CREATE TRIGGER update_role_permissions_updated_at
    BEFORE UPDATE ON role_permissions
    FOR EACH ROW
    EXECUTE FUNCTION update_updated_at_column();

-- SUPER_ADMIN: All permissions
INSERT INTO role_permissions (role_id, permission_id)
SELECT r.id, p.id
FROM roles r, permissions p
WHERE r.name = 'SUPER_ADMIN' AND r.is_system = TRUE;

-- PHARMACY_ADMIN: Tenant and user management, roles, dashboard
INSERT INTO role_permissions (role_id, permission_id)
SELECT r.id, p.id
FROM roles r, permissions p
WHERE r.name = 'PHARMACY_ADMIN' AND r.is_system = TRUE
  AND p.name IN (
      'TENANT_READ', 'TENANT_UPDATE',
      'USER_CREATE', 'USER_READ', 'USER_UPDATE', 'USER_DELETE', 'USER_STATUS_CHANGE', 'USER_ROLE_ASSIGN', 'USER_PASSWORD_RESET',
      'ROLE_READ', 'ROLE_CREATE', 'ROLE_UPDATE', 'ROLE_DELETE', 'ROLE_PERMISSION_ASSIGN',
      'PERMISSION_READ',
      'AUDIT_LOG_READ',
      'DASHBOARD_VIEW'
  );

-- PHARMACIST: User read, dashboard
INSERT INTO role_permissions (role_id, permission_id)
SELECT r.id, p.id
FROM roles r, permissions p
WHERE r.name = 'PHARMACIST' AND r.is_system = TRUE
  AND p.name IN (
      'USER_READ',
      'DASHBOARD_VIEW'
  );

-- PHARMACY_STAFF: Dashboard only
INSERT INTO role_permissions (role_id, permission_id)
SELECT r.id, p.id
FROM roles r, permissions p
WHERE r.name = 'PHARMACY_STAFF' AND r.is_system = TRUE
  AND p.name IN (
      'DASHBOARD_VIEW'
  );