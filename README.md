# Pharmacy Management SaaS

A commercial-grade, multi-tenant Pharmacy Management SaaS built as a modular monolith with Spring Boot 3.2+, Java 21, PostgreSQL, and Spring Security.

## Architecture Overview

```
┌─────────────────────────────────────────────────────────────────┐
│                      Spring Boot Modular Monolith               │
├─────────────────────────────────────────────────────────────────┤
│  ┌─────────┐  ┌─────────┐  ┌─────────┐  ┌─────────┐  ┌───────┐  │
│  │ Tenant  │  │  User   │  │  Role   │  │ Permis- │  │Audit  │  │
│  │ Module  │  │ Module  │  │ Module  │  │ sion    │  │ Module│  │
│  └────┬────┘  └────┬────┘  └────┬────┘  └────┬────┘  └───┬───┘  │
│       │            │            │            │            │      │
│       └────────────┴────────────┴────────────┴────────────┘      │
│                              │                                    │
│              ┌───────────────┴───────────────┐                   │
│              │      Common Module            │                   │
│              │  (BaseEntity, Repositories,   │                   │
│              │   Exception Handling, Utils)  │                   │
│              └───────────────┬───────────────┘                   │
│                              │                                    │
│              ┌───────────────┴───────────────┐                   │
│              │      Security Module          │                   │
│              │  (Auth, Session, CSRF, Rate   │                   │
│              │   Limiting, Password Reset)   │                   │
│              └───────────────┬───────────────┘                   │
│                              │                                    │
│              ┌───────────────┴───────────────┐                   │
│              │      Database (PostgreSQL)    │                   │
│              │  (Flyway Migrations V1-V8)    │                   │
│              └───────────────────────────────┘                   │
└─────────────────────────────────────────────────────────────────┘
```

## Technology Stack

| Component | Version |
|-----------|---------|
| Java | 21 |
| Spring Boot | 3.2.5 |
| Spring Security | 6.2 |
| Spring Session JDBC | 3.2 |
| Spring Data JPA | 3.2 |
| PostgreSQL | 16 |
| Flyway | 10.17.0 |
| MapStruct | 1.5.5.Final |
| Lombok | 1.18.32 |
| SpringDoc OpenAPI | 2.3.0 |
| Gradle | 8.5 |

## Security Features

- **Session-based Authentication** (no JWT) - HttpOnly, Secure, SameSite=Strict cookies
- **CSRF Protection** - Double-submit cookie pattern
- **Rate Limiting** - Bucket4j (5 req/min on auth endpoints)
- **Account Lockout** - 5 failed attempts → 30 min lockout
- **Session Rotation** - New session ID on each login
- **Concurrent Session Control** - Max 5 sessions per user
- **Password Reset** - SHA-256 hashed tokens, 15-min expiry, single-use
- **Multi-tenancy** - 5-layer tenant isolation

## Multi-tenancy

**Strategy**: Shared Database, Shared Schema with `tenant_id` column

**Isolation Layers**:
1. **ThreadLocal Context** - `TenantContextHolder` stores current tenant
2. **Repository Layer** - `BaseRepository` auto-appends `tenant_id` to queries
3. **Service Layer** - Validation services enforce tenant boundaries
4. **Controller Layer** - `@PreAuthorize` with tenant checks
5. **Database** - Row-level security ready (PostgreSQL RLS)

## Role-Based Access Control

| Role | Description | Key Permissions |
|------|-------------|-----------------|
| `SUPER_ADMIN` | Full system access | All 23 permissions |
| `PHARMACY_ADMIN` | Tenant management | Tenant read/update, User CRUD, Role CRUD, Dashboard |
| `PHARMACIST` | Pharmacy operations | User read, Dashboard |
| `PHARMACY_STAFF` | Limited access | Dashboard only |

## API Endpoints

### Authentication
| Method | Endpoint | Description |
|--------|----------|-------------|
| POST | `/api/auth/login` | Login (form data) |
| POST | `/api/auth/logout` | Logout |
| GET | `/api/auth/me` | Current user profile |
| POST | `/api/auth/change-password` | Change password |
| POST | `/api/auth/forgot-password` | Request password reset |
| POST | `/api/auth/reset-password` | Reset password with token |
| GET | `/api/auth/csrf` | Get CSRF token |

### Tenant Management (SUPER_ADMIN)
| Method | Endpoint | Description |
|--------|----------|-------------|
| POST | `/api/tenants` | Create tenant |
| GET | `/api/tenants` | Search tenants (paginated) |
| GET | `/api/tenants/{id}` | Get tenant |
| PUT | `/api/tenants/{id}` | Update tenant |
| PATCH | `/api/tenants/{id}/status` | Change status |
| DELETE | `/api/tenants/{id}` | Delete tenant |

### User Management (SUPER_ADMIN, PHARMACY_ADMIN)
| Method | Endpoint | Description |
|--------|----------|-------------|
| POST | `/api/users` | Create user |
| GET | `/api/users` | Search users (paginated) |
| GET | `/api/users/me` | Current user profile |
| GET | `/api/users/{id}` | Get user |
| PUT | `/api/users/{id}` | Update user |
| PATCH | `/api/users/{id}/status` | Change status |
| PATCH | `/api/users/{id}/roles` | Assign roles |
| POST | `/api/users/change-password` | Change password |
| DELETE | `/api/users/{id}` | Delete user |

### Role Management
| Method | Endpoint | Access |
|--------|----------|--------|
| GET | `/api/roles` | SUPER_ADMIN, PHARMACY_ADMIN |
| POST | `/api/roles` | SUPER_ADMIN |
| PUT | `/api/roles/{id}` | SUPER_ADMIN |
| DELETE | `/api/roles/{id}` | SUPER_ADMIN |

### Permission Catalog (SUPER_ADMIN)
| Method | Endpoint | Description |
|--------|----------|-------------|
| GET | `/api/permissions` | List all permissions |

### Audit Logs (SUPER_ADMIN)
| Method | Endpoint | Description |
|--------|----------|-------------|
| GET | `/api/audit-logs` | Search audit logs |
| GET | `/api/audit-logs/actions` | Distinct actions |
| GET | `/api/audit-logs/entity-types` | Distinct entity types |

### Dashboard
| Method | Endpoint | Description |
|--------|----------|-------------|
| GET | `/api/dashboard/stats` | Dashboard statistics |

## Quick Start

### Prerequisites
- Java 21
- PostgreSQL 16
- Gradle 8.5+

### Database Setup
```sql
CREATE DATABASE pharmacy_saas_dev;
CREATE USER pharmacy_user WITH ENCRYPTED PASSWORD 'pharmacy_pass';
GRANT ALL PRIVILEGES ON DATABASE pharmacy_saas_dev TO pharmacy_user;
```

### Running the Application
```bash
# Using Gradle Wrapper (recommended)
./gradlew bootRun --args='--spring.profiles.active=dev'

# Or build and run JAR
./gradlew build
java -jar build/libs/pharmacy-saas-1.0.0-SNAPSHOT.jar --spring.profiles.active=dev
```

### Access Points
- **API Base**: `http://localhost:8080/api`
- **Swagger UI**: `http://localhost:8080/swagger-ui.html`
- **OpenAPI Docs**: `http://localhost:8080/v3/api-docs`
- **Health Check**: `http://localhost:8080/actuator/health`

### Default Credentials (after migration)
The system roles are seeded via Flyway migration V5. Create a SUPER_ADMIN user manually or via database:

```sql
-- Create SUPER_ADMIN tenant
INSERT INTO tenants (tenant_code, pharmacy_name, email, status) 
VALUES ('SYSTEM', 'System Tenant', 'admin@system.com', 'ACTIVE');

-- Create SUPER_ADMIN user
INSERT INTO users (tenant_id, first_name, last_name, email, password_hash, status, pwd_changed_at)
SELECT id, 'Super', 'Admin', 'superadmin@example.com', 
       '$2a$12$...', -- BCrypt hash of 'Admin@123'
       'ACTIVE', CURRENT_TIMESTAMP
FROM tenants WHERE tenant_code = 'SYSTEM';

-- Assign SUPER_ADMIN role
INSERT INTO user_roles (user_id, role_id)
SELECT u.id, r.id
FROM users u, roles r
WHERE u.email = 'superadmin@example.com' AND r.name = 'SUPER_ADMIN';
```

## Project Structure

```
src/main/java/com/pharmacy/saas/
├── PharmacySaasApplication.java
├── common/
│   ├── entity/          # BaseEntity, TenantAwareEntity
│   ├── exception/       # BusinessException, ResourceNotFoundException, etc.
│   ├── repository/      # BaseRepository, TenantAwareRepository
│   ├── response/        # ApiResponse, PageResponse, ErrorDetail
│   ├── constants/       # ApplicationConstants, ErrorCodes, AuditActions
│   └── util/            # TenantContextHolder, PasswordUtils, TokenGenerator
├── config/
│   ├── WebConfig.java
│   ├── JacksonConfig.java
│   ├── OpenApiConfig.java
│   └── DataInitializer.java
├── security/
│   ├── config/          # SecurityConfig, SessionConfig, CorsConfig, PasswordEncoderConfig
│   ├── filter/          # TenantContextFilter, AuditLoggingFilter
│   ├── handler/         # CustomAuthenticationSuccessHandler, etc.
│   ├── service/         # AuthenticationService, PasswordResetService, etc.
│   ├── session/         # SessionRegistry, SessionManagementService
│   ├── entity/          # PasswordResetToken
│   └── repository/      # PasswordResetTokenRepository
├── tenant/
│   ├── entity/          # Tenant
│   ├── repository/      # TenantRepository
│   ├── service/         # TenantService, TenantValidationService
│   ├── controller/      # TenantController
│   └── dto/             # TenantRequest, TenantResponse, TenantSearchCriteria
├── user/
│   ├── entity/          # User, UserStatus, UserRole
│   ├── repository/      # UserRepository
│   ├── service/         # UserService, UserValidationService
│   ├── controller/      # UserController
│   └── dto/             # UserRequest, UserResponse, etc.
├── role/
│   ├── entity/          # Role, RolePermission
│   ├── repository/      # RoleRepository
│   ├── service/         # RoleService
│   ├── controller/      # RoleController
│   └── dto/             # RoleRequest, RoleResponse
├── permission/
│   ├── entity/          # Permission
│   ├── repository/      # PermissionRepository
│   ├── service/         # PermissionService
│   ├── controller/      # PermissionController
│   └── dto/             # PermissionResponse
├── audit/
│   ├── entity/          # AuditLog
│   ├── repository/      # AuditLogRepository
│   ├── service/         # AuditService, AuditEventPublisher
│   ├── controller/      # AuditController
│   └── dto/             # AuditLogResponse
└── dashboard/
    ├── service/         # DashboardService
    ├── controller/      # DashboardController
    └── dto/             # DashboardStatsResponse
```

## Database Migrations (Flyway)

| Version | Script | Description |
|---------|--------|-------------|
| V1 | `V1__create_tenants.sql` | Tenants table |
| V2 | `V2__create_users.sql` | Users + password_reset_tokens |
| V3 | `V3__create_roles_and_permissions.sql` | Roles + Permissions (system roles seeded) |
| V4 | `V4__create_user_roles.sql` | User-Roles junction |
| V5 | `V5__create_role_permissions.sql` | Role-Permissions + system role matrix |
| V6 | `V6__create_audit_logs.sql` | Audit logs with JSONB metadata |
| V7 | `V7__create_spring_session_tables.sql` | Spring Session tables |

## Configuration Profiles

| Profile | File | Use Case |
|---------|------|----------|
| `dev` | `application-dev.yml` | Local development |
| `prod` | `application-prod.yml` | Production |
| `test` | `application-test.yml` | Testcontainers integration tests |

## Testing

```bash
# Run all tests
./gradlew test

# Run unit tests only
./gradlew test --tests "*unit*"

# Run integration tests
./gradlew test --tests "*integration*"
```

### Test Infrastructure
- **Testcontainers**: PostgreSQL 16 for integration tests
- **Test Data Builder**: Fluent builders for test entities
- **Security Tests**: Rate limiting, CSRF, session rotation, cross-tenant access

## Architecture Decision Records (ADRs)

- [ADR-001: Session-based Authentication](docs/adr/001-session-based-auth.md)
- [ADR-002: Tenant Isolation Strategy](docs/adr/002-tenant-isolation-strategy.md)
- [ADR-003: Shared Database Shared Schema](docs/adr/003-shared-database-shared-schema.md)
- [ADR-004: BCrypt Password Hashing](docs/adr/004-bcrypt-password-hashing.md)

## Building for Production

```bash
./gradlew build -x test
docker build -t pharmacy-saas:latest .
```

## License

Proprietary - Pharmacy Management SaaS