-- V3: Create roles and permissions tables

CREATE TABLE permissions (
    id UUID PRIMARY KEY DEFAULT gen_random_uuid(),
    name VARCHAR(100) NOT NULL UNIQUE,
    description VARCHAR(500),
    created_at TIMESTAMPTZ NOT NULL DEFAULT CURRENT_TIMESTAMP,
    updated_at TIMESTAMPTZ NOT NULL DEFAULT CURRENT_TIMESTAMP,
    version BIGINT NOT NULL DEFAULT 0
);

CREATE TABLE roles (
    id UUID PRIMARY KEY DEFAULT gen_random_uuid(),
    name VARCHAR(50) NOT NULL,
    description VARCHAR(500),
    is_system BOOLEAN NOT NULL DEFAULT FALSE,
    tenant_id UUID REFERENCES tenants(id) ON DELETE CASCADE,
    created_at TIMESTAMPTZ NOT NULL DEFAULT CURRENT_TIMESTAMP,
    updated_at TIMESTAMPTZ NOT NULL DEFAULT CURRENT_TIMESTAMP,
    version BIGINT NOT NULL DEFAULT 0
);

CREATE UNIQUE INDEX idx_roles_tenant_name ON roles(tenant_id, name) WHERE tenant_id IS NOT NULL;
CREATE UNIQUE INDEX idx_roles_system_name ON roles(name) WHERE is_system = TRUE AND tenant_id IS NULL;

CREATE INDEX idx_roles_tenant_id ON roles(tenant_id);
CREATE INDEX idx_roles_is_system ON roles(is_system);

CREATE TRIGGER update_roles_updated_at
    BEFORE UPDATE ON roles
    FOR EACH ROW
    EXECUTE FUNCTION update_updated_at_column();

CREATE TRIGGER update_permissions_updated_at
    BEFORE UPDATE ON permissions
    FOR EACH ROW
    EXECUTE FUNCTION update_updated_at_column();

-- Insert system permissions
INSERT INTO permissions (name, description) VALUES
    ('TENANT_CREATE', 'Create new tenants'),
    ('TENANT_READ', 'View tenant details'),
    ('TENANT_UPDATE', 'Update tenant details'),
    ('TENANT_DELETE', 'Delete tenants'),
    ('TENANT_STATUS_CHANGE', 'Change tenant status'),
    ('USER_CREATE', 'Create new users'),
    ('USER_READ', 'View user details'),
    ('USER_UPDATE', 'Update user details'),
    ('USER_DELETE', 'Delete users'),
    ('USER_STATUS_CHANGE', 'Change user status'),
    ('USER_ROLE_ASSIGN', 'Assign roles to users'),
    ('USER_PASSWORD_RESET', 'Reset user passwords'),
    ('ROLE_CREATE', 'Create new roles'),
    ('ROLE_READ', 'View role details'),
    ('ROLE_UPDATE', 'Update role details'),
    ('ROLE_DELETE', 'Delete roles'),
    ('ROLE_PERMISSION_ASSIGN', 'Assign permissions to roles'),
    ('PERMISSION_CREATE', 'Create new permissions'),
    ('PERMISSION_READ', 'View permission details'),
    ('PERMISSION_UPDATE', 'Update permission details'),
    ('PERMISSION_DELETE', 'Delete permissions'),
    ('AUDIT_LOG_READ', 'View audit logs'),
    ('DASHBOARD_VIEW', 'View dashboard statistics');

-- Insert system roles
INSERT INTO roles (id, name, description, is_system, tenant_id) VALUES
    (gen_random_uuid(), 'SUPER_ADMIN', 'Super Administrator with full system access', TRUE, NULL),
    (gen_random_uuid(), 'PHARMACY_ADMIN', 'Pharmacy Administrator with tenant management access', TRUE, NULL),
    (gen_random_uuid(), 'PHARMACIST', 'Pharmacist with pharmacy operations access', TRUE, NULL),
    (gen_random_uuid(), 'PHARMACY_STAFF', 'Pharmacy Staff with limited access', TRUE, NULL);