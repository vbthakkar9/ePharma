# Pharmacy Management SaaS - Architecture Plan

## Context
Building a commercial-grade, multi-tenant Pharmacy Management SaaS application from scratch. The application supports multiple pharmacies (tenants), each with their own users, branches, roles, permissions, and business data. Starting with a modular monolithic architecture that can evolve as the business grows.

**Current scope (Phases 1-8):**
1. Tenant Management
2. User Management
3. Role and Permission Management
4. Login and Authentication Management
5. Session Management
6. Authorization and Security

---

## Phase 1: Architecture Design

### 1.1 Overall Architecture

```
┌─────────────────────────────────────────────────────────────────────────────┐
│                         PHARMACY MANAGEMENT SAAS                            │
├─────────────────────────────────────────────────────────────────────────────┤
│                                                                             │
│  ┌──────────────┐    ┌──────────────────────┐    ┌────────────────────┐   │
│  │   React      │    │   Spring Boot        │    │   PostgreSQL       │   │
│  │   Frontend   │◄──►│   Backend (Modular   │◄──►│   Database         │   │
│  │   (Vite,     │    │   Monolith)         │    │   (Shared Schema,  │   │
│  │   TypeScript,│    │                    │    │   tenant_id        │   │
│  │   MUI,       │    │  ┌──────────────┐   │    │   isolation)       │   │
│  │   Axios)     │    │  │ Tenant       │   │    │                    │   │
│  └──────────────┘    │  │ Module       │   │    │  ┌──────────────┐  │   │
│                      │  │ User Module  │   │    │  │ tenants      │  │   │
│       HTTPS          │  │ Role Module  │   │    │  │ users        │  │   │
│  Secure Cookies      │  │ Permission   │   │    │  │ roles        │  │   │
│  (HttpOnly,          │  │ Module       │   │    │  │ permissions  │  │   │
│   SameSite=Strict)   │  │ Auth Module  │   │    │  │ user_roles   │  │   │
│                      │  │ Audit Module │   │    │  │ role_perms   │  │   │
│  CSRF Protection     │  │ Common       │   │    │  │ reset_tokens │  │   │
│                      │  │ Config       │   │    │  │ audit_logs   │  │   │
│                      │  │ Security     │   │    │  │ spring_session│  │   │
│                      │  └──────────────┘   │    │  └──────────────┘  │   │
│                      └──────────────────────┘    └────────────────────┘   │
│                                                                             │
└─────────────────────────────────────────────────────────────────────────────┘
```

### 1.2 Module Responsibilities

| Module | Responsibility | Key Components |
|--------|---------------|----------------|
| **common** | Cross-cutting concerns | Exception handling, API responses, constants, utilities, global error handler |
| **security** | Authentication & authorization | Spring Security config, session management, CSRF, password encoding, rate limiting |
| **tenant** | Tenant lifecycle | CRUD, status management, tenant context resolution, isolation enforcement |
| **user** | User management within tenant | CRUD, role assignment, password management, status, search/pagination |
| **role** | Role management | CRUD, role-permission mapping, default role seeding |
| **permission** | Permission registry | CRUD, permission catalog, assignment to roles |
| **audit** | Audit logging | Async event logging, immutable audit records, querying |
| **dashboard** | Aggregated views | Tenant/user stats, login metrics (future) |

### 1.3 Tenant Isolation Strategy

**Approach: Shared Database, Shared Schema, tenant_id Column**

```sql
-- Every tenant-owned table includes:
tenant_id UUID NOT NULL REFERENCES tenants(id)

-- Example query with automatic isolation:
SELECT * FROM users WHERE tenant_id = ? AND email = ?
```

**Enforcement Layers:**
1. **TenantContextHolder** - ThreadLocal storing current tenant_id from authenticated user
2. **Repository Level** - All queries auto-append `AND tenant_id = :tenantId` via base repository
3. **Service Level** - Validation that requested resources belong to current tenant
4. **Controller Level** - `@PreAuthorize` checks on tenant-scoped endpoints
5. **Future: Row Level Security (RLS)** - PostgreSQL policies as defense-in-depth

**Super Admin Access:** Bypasses tenant filter via `SecurityContext` role check (`hasRole('SUPER_ADMIN')`)

### 1.4 Session-Based Authentication Architecture

```
┌────────────────────────────────────────────────────────────────────┐
│                    SESSION AUTHENTICATION FLOW                      │
├────────────────────────────────────────────────────────────────────┤
│                                                                    │
│  CLIENT (React)                          SERVER (Spring Boot)      │
│  ─────────────                          ─────────────────────      │
│                                                                    │
│  1. POST /api/auth/login                                          │
│     { email, password }                                           │
│     ──────────────────────────────►                                │
│                                                                    │
│                                    2. Validate credentials         │
│                                    3. Create Spring Session        │
│                                    4. Store in PostgreSQL          │
│                                    5. Set SESSION cookie           │
│                                       (HttpOnly, Secure,           │
│                                        SameSite=Strict)            │
│                                    6. Rotate session ID            │
│     ◄──────────────────────────────                                │
│     200 OK + UserProfile + Roles                                   │
│     Set-Cookie: SESSION=xyz; HttpOnly; Secure; SameSite=Strict     │
│                                                                    │
│  7. Subsequent requests:                                           │
│     GET /api/users                                                 │
│     Cookie: SESSION=xyz ──────────►                                │
│                                    8. Resolve session from DB      │
│                                    9. Load SecurityContext         │
│                                    10. TenantContext from user     │
│     ◄──────────────────────────────                                │
│     200 OK + Data                                                  │
│                                                                    │
│  11. POST /api/auth/logout                                         │
│     ──────────────────────────────►                                │
│                                    12. Invalidate session in DB    │
│                                    13. Clear cookie                │
│     ◄──────────────────────────────                                │
│     200 OK                                                         │
│                                                                    │
└────────────────────────────────────────────────────────────────────┘
```

**Key Security Decisions:**
- **No JWT** - Sessions stored server-side in PostgreSQL via Spring Session JDBC
- **HttpOnly Cookies** - Prevents XSS token theft
- **Secure + SameSite=Strict** - CSRF mitigation + cookie theft prevention
- **CSRF Protection** - Spring Security CSRF tokens (double-submit cookie pattern)
- **Session Rotation** - New session ID on login, prevents fixation
- **Concurrent Session Control** - Configurable max sessions per user
- **Idle/Max Timeout** - Configurable (default 30 min idle, 8 hours max)

### 1.5 Database Design

#### Entity Relationship Diagram

```
┌─────────────┐       ┌─────────────┐       ┌─────────────┐
│   tenants   │       │    users    │       │    roles    │
├─────────────┤       ├─────────────┤       ├─────────────┤
│ id (PK)     │◄──────│ id (PK)     │       │ id (PK)     │
│ tenant_code │       │ tenant_id   │       │ name        │
│ pharmacy_nm │       │ first_name  │       │ description │
│ email       │       │ last_name   │       │ is_system   │
│ mobile      │       │ email       │       │ created_at  │
│ address     │       │ mobile      │       └──────┬──────┘
│ city        │       │ password_hash            │
│ state       │       │ status          ┌────────┴────────┐
│ country     │       │ last_login_at   │               │
│ postal_code │       │ pwd_changed_at  ▼               ▼
│ status      │       │ created_at   ┌─────────┐  ┌─────────────┐
│ sub_status  │       │ updated_at   │user_role│  │role_perm    │
│ created_at  │       └──────┬────────└────┬────┘  │  ┌────────┐ │
│ updated_at  │              │             │        │  │ id (PK)│ │
└─────────────┘              │             │        │  │ role_id│ │
                             │             │        │  │ perm_id│ │
                    ┌────────┴────────┐    │        │  └────────┘ │
                    │ password_reset  │    │        └─────────────┘
                    ├─────────────────┤    │
                    │ id (PK)         │    │
                    │ user_id (FK)    │    │
                    │ token_hash      │    │
                    │ expires_at      │    │
                    │ used            │    │
                    │ created_at      │    │
                    └─────────────────┘    │
                                         ▼
                          ┌─────────────────────────┐
                          │      audit_logs         │
                          ├─────────────────────────┤
                          │ id (PK)                 │
                          │ tenant_id (FK, nullable)│
                          │ user_id (FK, nullable)  │
                          │ action                  │
                          │ entity_type             │
                          │ entity_id               │
                          │ metadata (JSONB)        │
                          │ ip_address              │
                          │ created_at              │
                          └─────────────────────────┘
```

#### Table Details

| Table | Primary Key | Foreign Keys | Key Indexes |
|-------|-------------|--------------|-------------|
| tenants | id (UUID) | - | tenant_code (unique), email (unique) |
| users | id (UUID) | tenant_id → tenants | email+tenant_id (unique), tenant_id+status |
| roles | id (UUID) | - | name+tenant_id (unique) |
| permissions | id (UUID) | - | name (unique) |
| user_roles | (user_id, role_id) | user_id → users, role_id → roles | - |
| role_permissions | (role_id, permission_id) | role_id → roles, perm_id → permissions | - |
| password_reset_tokens | id (UUID) | user_id → users | user_id, token_hash, expires_at |
| audit_logs | id (UUID) | tenant_id → tenants, user_id → users | tenant_id+created_at, user_id+created_at |
| spring_session | session_id | - | expires_at, principal_name |

#### Spring Session Tables (auto-created by Spring Session JDBC)
```sql
-- SPRING_SESSION
-- SPRING_SESSION_ATTRIBUTES
```

### 1.6 Folder Structure

```
pharmacy-backend/
├── build.gradle
├── settings.gradle
├── gradle.properties
├── gradlew
├── gradlew.bat
└── src/
    ├── main/
    │   ├── java/com/pharmacy/saas/
    │   │   ├── PharmacySaasApplication.java
    │   │   │
    │   │   ├── config/
    │   │   │   ├── WebConfig.java
    │   │   │   ├── JacksonConfig.java
    │   │   │   └── OpenApiConfig.java
    │   │   │
    │   │   ├── common/
    │   │   │   ├── exception/
    │   │   │   │   ├── BusinessException.java
    │   │   │   │   ├── ResourceNotFoundException.java
    │   │   │   │   ├── ValidationException.java
    │   │   │   │   ├── DuplicateResourceException.java
    │   │   │   │   └── GlobalExceptionHandler.java
    │   │   │   ├── response/
    │   │   │   │   ├── ApiResponse.java
    │   │   │   │   ├── PageResponse.java
    │   │   │   │   └── ErrorDetail.java
    │   │   │   ├── constants/
    │   │   │   │   ├── ApplicationConstants.java
    │   │   │   │   ├── ErrorCodes.java
    │   │   │   │   └── AuditActions.java
    │   │   │   └── util/
    │   │   │       ├── TenantContextHolder.java
    │   │   │       ├── PasswordUtils.java
    │   │   │       └── TokenGenerator.java
    │   │   │
    │   │   ├── security/
    │   │   │   ├── config/
    │   │   │   │   ├── SecurityConfig.java
    │   │   │   │   ├── SessionConfig.java
    │   │   │   │   ├── CorsConfig.java
    │   │   │   │   └── PasswordEncoderConfig.java
    │   │   │   ├── filter/
    │   │   │   │   ├── TenantContextFilter.java
    │   │   │   │   └── AuditLoggingFilter.java
    │   │   │   ├── handler/
    │   │   │   │   ├── CustomAuthenticationSuccessHandler.java
    │   │   │   │   ├── CustomAuthenticationFailureHandler.java
    │   │   │   │   └── CustomLogoutSuccessHandler.java
    │   │   │   ├── service/
    │   │   │   │   ├── CustomUserDetailsService.java
    │   │   │   │   ├── AuthenticationService.java
    │   │   │   │   ├── PasswordResetService.java
    │   │   │   │   └── RateLimitingService.java
    │   │   │   └── session/
    │   │   │       ├── SessionRegistry.java
    │   │   │       └── SessionManagementService.java
    │   │   │
    │   │   ├── tenant/
    │   │   │   ├── controller/
    │   │   │   │   └── TenantController.java
    │   │   │   ├── service/
    │   │   │   │   ├── TenantService.java
    │   │   │   │   └── TenantValidationService.java
    │   │   │   ├── repository/
    │   │   │   │   └── TenantRepository.java
    │   │   │   ├── entity/
    │   │   │   │   └── Tenant.java
    │   │   │   └── dto/
    │   │   │       ├── TenantRequest.java
    │   │   │       ├── TenantResponse.java
    │   │   │       └── TenantSearchCriteria.java
    │   │   │
    │   │   ├── user/
    │   │   │   ├── controller/
    │   │   │   │   └── UserController.java
    │   │   │   ├── service/
    │   │   │   │   ├── UserService.java
    │   │   │   │   └── UserValidationService.java
    │   │   │   ├── repository/
    │   │   │   │   └── UserRepository.java
    │   │   │   ├── entity/
    │   │   │   │   ├── User.java
    │   │   │   │   └── UserStatus.java
    │   │   │   └── dto/
    │   │   │       ├── UserRequest.java
    │   │   │       ├── UserResponse.java
    │   │   │       ├── UserSearchCriteria.java
    │   │   │       ├── ChangePasswordRequest.java
    │   │   │       └── AssignRolesRequest.java
    │   │   │
    │   │   ├── role/
    │   │   │   ├── controller/
    │   │   │   │   └── RoleController.java
    │   │   │   ├── service/
    │   │   │   │   └── RoleService.java
    │   │   │   ├── repository/
    │   │   │   │   └── RoleRepository.java
    │   │   │   ├── entity/
    │   │   │   │   └── Role.java
    │   │   │   └── dto/
    │   │   │       ├── RoleRequest.java
    │   │   │       └── RoleResponse.java
    │   │   │
    │   │   ├── permission/
    │   │   │   ├── controller/
    │   │   │   │   └── PermissionController.java
    │   │   │   ├── service/
    │   │   │   │   └── PermissionService.java
    │   │   │   ├── repository/
    │   │   │   │   └── PermissionRepository.java
    │   │   │   ├── entity/
    │   │   │   │   └── Permission.java
    │   │   │   └── dto/
    │   │   │       └── PermissionResponse.java
    │   │   │
    │   │   ├── audit/
    │   │   │   ├── controller/
    │   │   │   │   └── AuditController.java
    │   │   │   ├── service/
    │   │   │   │   ├── AuditService.java
    │   │   │   │   └── AuditEventPublisher.java
    │   │   │   ├── repository/
    │   │   │   │   └── AuditLogRepository.java
    │   │   │   ├── entity/
    │   │   │   │   └── AuditLog.java
    │   │   │   └── dto/
    │   │   │       └── AuditLogResponse.java
    │   │   │
    │   │   └── dashboard/
    │   │       ├── controller/
    │   │       │   └── DashboardController.java
    │   │       └── service/
    │   │           └── DashboardService.java
    │   │
    │   └── resources/
    │       ├── application.yml
    │       ├── application-dev.yml
    │       ├── application-prod.yml
    │       └── db/migration/
    │           ├── V1__create_tenants.sql
    │           ├── V2__create_users.sql
    │           ├── V3__create_roles_and_permissions.sql
    │           ├── V4__create_user_roles.sql
    │           ├── V5__create_role_permissions.sql
    │           ├── V6__create_password_reset_tokens.sql
    │           ├── V7__create_audit_logs.sql
    │           └── V8__create_spring_session_tables.sql
    │
    └── test/
        ├── java/com/pharmacy/saas/
        │   ├── integration/
        │   │   ├── auth/
        │   │   ├── tenant/
        │   │   ├── user/
        │   │   └── security/
        │   ├── unit/
        │   │   ├── service/
        │   │   └── util/
        │   └── support/
        │       ├── TestDataBuilder.java
        │       └── TestContainersConfig.java
        └── resources/
            └── application-test.yml
```

### 1.7 API Endpoint Summary

| Module | Endpoint | Method | Auth | Roles |
|--------|----------|--------|------|-------|
| Auth | `/api/auth/login` | POST | Public | - |
| Auth | `/api/auth/logout` | POST | Session | USER |
| Auth | `/api/auth/me` | GET | Session | USER |
| Auth | `/api/auth/change-password` | POST | Session | USER |
| Auth | `/api/auth/forgot-password` | POST | Public | - |
| Auth | `/api/auth/reset-password` | POST | Public | - |
| Tenant | `/api/tenants` | POST | Session | SUPER_ADMIN |
| Tenant | `/api/tenants` | GET | Session | SUPER_ADMIN |
| Tenant | `/api/tenants/{id}` | GET | Session | SUPER_ADMIN, PHARMACY_ADMIN* |
| Tenant | `/api/tenants/{id}` | PUT | Session | SUPER_ADMIN, PHARMACY_ADMIN* |
| Tenant | `/api/tenants/{id}/status` | PATCH | Session | SUPER_ADMIN |
| User | `/api/users` | POST | Session | PHARMACY_ADMIN, SUPER_ADMIN |
| User | `/api/users` | GET | Session | PHARMACY_ADMIN, SUPER_ADMIN |
| User | `/api/users/{id}` | GET | Session | PHARMACY_ADMIN, SUPER_ADMIN, SELF |
| User | `/api/users/{id}` | PUT | Session | PHARMACY_ADMIN, SUPER_ADMIN |
| User | `/api/users/{id}/status` | PATCH | Session | PHARMACY_ADMIN, SUPER_ADMIN |
| User | `/api/users/{id}/roles` | PATCH | Session | PHARMACY_ADMIN, SUPER_ADMIN |
| Role | `/api/roles` | GET | Session | PHARMACY_ADMIN, SUPER_ADMIN |
| Role | `/api/roles` | POST | Session | SUPER_ADMIN |
| Role | `/api/roles/{id}` | PUT | Session | SUPER_ADMIN |
| Permission | `/api/permissions` | GET | Session | SUPER_ADMIN |
| Audit | `/api/audit-logs` | GET | Session | SUPER_ADMIN |

* PHARMACY_ADMIN can only access their own tenant

### 1.8 Default Roles & Permissions Matrix

| Permission | SUPER_ADMIN | PHARMACY_ADMIN | PHARMACIST | PHARMACY_STAFF |
|------------|-------------|----------------|------------|----------------|
| TENANT_CREATE | ✓ | ✗ | ✗ | ✗ |
| TENANT_VIEW | ✓ | Own only | ✗ | ✗ |
| TENANT_UPDATE | ✓ | Own only | ✗ | ✗ |
| TENANT_DELETE | ✓ | ✗ | ✗ | ✗ |
| USER_CREATE | ✓ | ✓ | ✗ | ✗ |
| USER_VIEW | ✓ | ✓ | ✓ | ✓ |
| USER_UPDATE | ✓ | ✓ | ✗ | ✗ |
| USER_DELETE | ✓ | ✓ | ✗ | ✗ |
| USER_ACTIVATE | ✓ | ✓ | ✗ | ✗ |
| USER_DEACTIVATE | ✓ | ✓ | ✗ | ✗ |
| ROLE_ASSIGN | ✓ | ✓ (limited) | ✗ | ✗ |
| ROLE_VIEW | ✓ | ✓ | ✓ | ✓ |
| PERMISSION_MANAGE | ✓ | ✗ | ✗ | ✗ |

### 1.9 Security Decisions Summary

| Decision | Rationale |
|----------|-----------|
| Session-based auth (no JWT) | Server-side revocation, simpler security model, Spring Session mature |
| BCrypt (cost 12) | Industry standard, configurable work factor, Spring Security native |
| HttpOnly + Secure + SameSite=Strict cookies | Defense in depth against XSS/CSRF |
| Spring Security CSRF (double-submit) | Built-in, works with session auth |
| Rate limiting on auth endpoints | Prevents brute force, credential stuffing |
| Account lockout after 5 failed attempts | Balance security vs UX, admin unlock required |
| Password reset tokens: SHA-256 hashed, 15-min expiry, single-use | Token not stored in plaintext, short window, replay protection |
| Audit logging async via @Async | Non-blocking, survives transaction rollback |
| TenantContext via ThreadLocal + Filter | Transparent to services, enforceable at repo level |
| Optimistic locking on entities | Prevents lost updates in concurrent edits |

---

## Phase 2-8: Implementation Roadmap

### Phase 2: Backend Foundation (Week 1)
- Spring Boot project setup with Gradle Groovy DSL
- PostgreSQL + Flyway configuration
- Common exception handling & API responses
- OpenAPI/Swagger setup
- Base entity & repository classes

### Phase 3: Database (Week 1-2)
- Entity classes with JPA annotations
- Flyway migration scripts (V1-V8)
- Repository interfaces with tenant-aware queries
- Indexes, constraints, optimistic locking

### Phase 4: Authentication (Week 2)
- Spring Security configuration
- Spring Session JDBC setup
- Login/Logout handlers
- Password encoding (BCrypt)
- CSRF, CORS, Rate limiting
- Password reset flow
- Session management

### Phase 5: Tenant & User Management (Week 3)
- Tenant CRUD + status management
- User CRUD + role assignment
- Role & Permission management
- Tenant isolation enforcement
- Search, pagination, validation

### Phase 6: Frontend (Week 3-4)
- Vite + React + TypeScript + MUI setup
- Axios interceptor with credentials
- Login, Forgot/Reset Password screens
- Dashboard with role-based navigation
- User management (list, create, edit, view)
- Tenant management (Super Admin)
- Role management
- Profile & Change Password

### Phase 7: Testing & Security (Week 4)
- Unit tests (services, utils)
- Integration tests (Testcontainers)
- Security tests (auth, authorization, isolation)
- Tenant isolation verification
- Session security tests

### Phase 8: Documentation (Week 4)
- README with setup instructions
- Architecture decision records
- Database schema documentation
- API documentation (OpenAPI)
- Docker Compose for local dev

---

## Verification Criteria

### Phase 1 Complete When:
- [ ] Architecture document reviewed and approved
- [ ] Database design validated
- [ ] Security model confirmed
- [ ] Module boundaries agreed

### Phase 2 Complete When:
- [ ] `./gradlew build` succeeds
- [ ] Spring Boot starts with dev profile
- [ ] Flyway migrations apply cleanly
- [ ] Swagger UI accessible at `/swagger-ui.html`

### Phase 3 Complete When:
- [ ] All entities compile
- [ ] Repositories pass basic CRUD tests
- [ ] Schema matches ERD
- [ ] Indexes created

### Phase 4 Complete When:
- [ ] Login returns session cookie
- [ ] `/api/auth/me` returns user profile
- [ ] Logout invalidates session
- [ ] CSRF token required for state changes
- [ ] Rate limiting blocks after 5 failures
- [ ] Password reset flow works end-to-end

### Phase 5 Complete When:
- [ ] Super Admin can CRUD tenants
- [ ] Pharmacy Admin can CRUD users in own tenant
- [ ] Role assignment works with permissions
- [ ] Tenant isolation verified (cross-tenant queries return 403/empty)

### Phase 6 Complete When:
- [ ] Login page renders, submits to backend
- [ ] Protected routes redirect to login
- [ ] Dashboard shows role-appropriate menu
- [ ] User list loads with pagination/search
- [ ] Create/Edit user forms validate
- [ ] Toast notifications on actions

### Phase 7 Complete When:
- [ ] Unit test coverage > 80% on services
- [ ] Integration tests pass with Testcontainers
- [ ] No critical security findings
- [ ] Load test: 100 concurrent sessions stable

### Phase 8 Complete When:
- [ ] README enables fresh checkout → running app
- [ ] Docker Compose starts all services
- [ ] API docs generated and accurate

---

## Open Questions for Clarification - RESOLVED

1. **Email Uniqueness Scope**: **Per-tenant unique** (`UNIQUE(tenant_id, email)`). Super Admin users managed separately.

2. **Super Admin Tenant**: **Super Admin is above all tenants** - not tied to any tenant. Will have `tenant_id = NULL` with special handling in TenantContext. Separate platform-level user management.

3. **Password Policy**: **No strict policy for now** - basic validation only (min 8 chars). Can be enhanced later.

4. **Session Timeouts**: **Max session age: 8 hours**. Idle timeout: 30 minutes (default).

5. **Concurrent Sessions**: **Yes, max 5 sessions per user**. Excess invalidates oldest session.

6. **Audit Log Retention**: **Never delete audit logs**. Immutable audit trail. Consider partitioning by year for performance.

7. **Frontend**: **Do not generate frontend code for now**. Backend-only implementation.

8. **Database Naming**: To be configured in `application-dev.yml` (e.g., `pharmacy_saas_dev`, user `pharmacy_user`).

---