# ADR 003: Shared Database, Shared Schema

## Status
Accepted

## Context
Following ADR 002 (tenant isolation strategy), this ADR documents the specific database architecture decision.

## Decision
Use a **single PostgreSQL database** with a **single schema (public)** where all tenant-owned tables include a `tenant_id` column referencing the `tenants` table.

## Schema Design

### Core Tables
```sql
-- Tenants (no tenant_id - they are the root)
CREATE TABLE tenants (
    id UUID PRIMARY KEY DEFAULT gen_random_uuid(),
    tenant_code VARCHAR(50) UNIQUE NOT NULL,
    pharmacy_name VARCHAR(255) NOT NULL,
    email VARCHAR(255) UNIQUE NOT NULL,
    ...
);

-- All tenant-owned tables include tenant_id
CREATE TABLE users (
    id UUID PRIMARY KEY DEFAULT gen_random_uuid(),
    tenant_id UUID NOT NULL REFERENCES tenants(id),
    email VARCHAR(255) NOT NULL,
    ...
    UNIQUE (tenant_id, email)  -- Per-tenant uniqueness
);

CREATE TABLE roles (
    id UUID PRIMARY KEY DEFAULT gen_random_uuid(),
    tenant_id UUID REFERENCES tenants(id),  -- NULL for system roles
    name VARCHAR(100) NOT NULL,
    is_system BOOLEAN DEFAULT FALSE,
    ...
    UNIQUE (tenant_id, name) WHERE tenant_id IS NOT NULL
);

-- Global tables (no tenant_id)
CREATE TABLE permissions (
    id UUID PRIMARY KEY DEFAULT gen_random_uuid(),
    name VARCHAR(100) UNIQUE NOT NULL,
    ...
);
```

### Indexing Strategy
```sql
-- Tenant-scoped queries
CREATE INDEX idx_users_tenant_id ON users(tenant_id);
CREATE INDEX idx_users_tenant_email ON users(tenant_id, email);
CREATE INDEX idx_roles_tenant_id ON roles(tenant_id);

-- Audit queries
CREATE INDEX idx_audit_logs_tenant_created ON audit_logs(tenant_id, created_at DESC);
CREATE INDEX idx_audit_logs_user_created ON audit_logs(user_id, created_at DESC);

-- JSONB metadata queries
CREATE INDEX idx_audit_logs_metadata ON audit_logs USING GIN (metadata);
```

## UUID Primary Keys

Using `gen_random_uuid()` for all primary keys:
- Globally unique (no coordination needed)
- No information leakage (unlike sequential IDs)
- Compatible with distributed systems
- PostgreSQL native support

## Soft Deletes

Not implemented initially. Consider for future:
```sql
ALTER TABLE users ADD COLUMN deleted_at TIMESTAMPTZ;
CREATE INDEX idx_users_deleted_at ON users(deleted_at) WHERE deleted_at IS NOT NULL;
```

## Partitioning

Audit logs table designed for future partitioning by year:
```sql
-- When needed:
CREATE TABLE audit_logs_2024 PARTITION OF audit_logs
    FOR VALUES FROM ('2024-01-01') TO ('2025-01-01');
```

## Consequences

### Positive
- Simple operational model (one DB, one schema)
- Easy cross-tenant analytics for Super Admin
- Single Flyway migration path
- Cost-effective (single database instance)
- ACID transactions across tenant boundaries when needed

### Negative
- Noisy neighbor problem (one tenant's load affects others)
- Schema changes affect all tenants
- Maximum database size limits total tenants
- Backup/restore is all-or-nothing

## Mitigation Strategies

1. **Connection Pooling**: HikariCP with appropriate limits
2. **Read Replicas**: For reporting queries
3. **Archiving**: Move old audit logs to cold storage
4. **Monitoring**: Per-tenant query performance tracking

## Future Evolution

| Trigger | Evolution |
|---------|-----------|
| >100 tenants | Add read replicas |
| >1000 tenants | Schema-per-tenant with routing |
| Large tenant | Dedicated database |
| Compliance | Separate schema/database per region |

## References
- [Citus Data Multi-tenancy](https://www.citus.io/blog/2019/08/12/multi-tenancy-postgres/)
- [PostgreSQL Partitioning](https://www.postgresql.org/docs/current/ddl-partitioning.html)