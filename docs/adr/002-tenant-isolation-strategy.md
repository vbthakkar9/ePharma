# ADR-002: Tenant Isolation Strategy

## Status
Accepted

## Context
In a multi-tenant SaaS, ensuring strict data isolation between tenants is critical. We need a defense-in-depth approach to prevent cross-tenant data access.

## Decision
Implement **5-layer tenant isolation** with Shared Database, Shared Schema strategy.

## Five-Layer Isolation Model

### Layer 1: ThreadLocal Context (`TenantContextHolder`)
```java
public class TenantContextHolder {
    private static final ThreadLocal<UUID> tenantId = new ThreadLocal<>();
    private static final ThreadLocal<UUID> userId = new ThreadLocal<>();
    
    public static void setTenantId(UUID id) { tenantId.set(id); }
    public static UUID getTenantId() { return tenantId.get(); }
    public static UUID getRequiredTenantId() {
        UUID id = tenantId.get();
        if (id == null) throw new IllegalStateException("Tenant context not set");
        return id;
    }
    public static boolean isSuperAdmin() { /* check roles */ }
    public static void clear() { /* clear all */ }
}
```

**Enforcement**: `TenantContextFilter` runs on every request, extracts tenant from authenticated user.

### Layer 2: Repository Layer (`BaseRepository`)
```java
public interface BaseRepository<T, ID> extends JpaRepository<T, ID> {
    
    @Query("SELECT e FROM #{#entityName} e WHERE e.tenantId = :tenantId")
    List<T> findAllByTenantId(@Param("tenantId") UUID tenantId);
    
    @Query("SELECT e FROM #{#entityName} e WHERE e.id = :id AND e.tenantId = :tenantId")
    Optional<T> findByIdAndTenantId(@Param("id") ID id, @Param("tenantId") UUID tenantId);
    
    // Super Admin bypass
    default Optional<T> findByIdAnyTenant(ID id) {
        return findById(id); // No tenant filter
    }
}
```

**Auto-application**: All tenant-aware entities extend `TenantAwareEntity` with `tenantId` field.

### Layer 3: Service Layer Validation
```java
@Service
public class UserValidationService {
    public User getUserOrThrow(UUID id) {
        UUID tenantId = TenantContextHolder.getRequiredTenantId();
        return userRepository.findByIdAndTenantId(id, tenantId)
            .orElseThrow(() -> new ResourceNotFoundException("User", id));
    }
    
    public void validateTenantAccess(UUID resourceTenantId) {
        UUID currentTenant = TenantContextHolder.getTenantId();
        if (currentTenant != null && !currentTenant.equals(resourceTenantId)) {
            throw new AccessDeniedException("Cross-tenant access denied");
        }
    }
}
```

### Layer 4: Controller Authorization
```java
@RestController
@RequestMapping("/api/users")
public class UserController {
    
    @GetMapping("/{id}")
    @PreAuthorize("hasAnyRole('SUPER_ADMIN', 'PHARMACY_ADMIN') or @userSecurityService.isSelf(#id)")
    public ResponseEntity<ApiResponse<UserResponse>> getUser(@PathVariable UUID id) {
        // Service layer validates tenant access
        return ResponseEntity.ok(userService.getUser(id));
    }
}
```

Custom security expressions:
```java
@Component("userSecurityService")
public class UserSecurityService {
    public boolean isSelf(UUID userId) {
        return TenantContextHolder.getUserId().equals(userId);
    }
    
    public boolean hasTenantAccess(UUID resourceTenantId) {
        UUID currentTenant = TenantContextHolder.getTenantId();
        return currentTenant != null && currentTenant.equals(resourceTenantId);
    }
}
```

### Layer 5: Database Row-Level Security (Future)
PostgreSQL RLS policies for defense-in-depth:
```sql
-- Enable RLS
ALTER TABLE users ENABLE ROW LEVEL SECURITY;

-- Policy: Users can only see rows from their tenant
CREATE POLICY tenant_isolation ON users
    USING (tenant_id = current_setting('app.current_tenant_id')::uuid);

-- Set tenant context on connection
SET app.current_tenant_id = '...';
```

## Super Admin Bypass

Super Admin users bypass tenant filtering at Layer 2:
```java
@Query("SELECT e FROM #{#entityName} e WHERE " +
       "(:isSuperAdmin = true OR e.tenantId = :tenantId) AND e.id = :id")
Optional<T> findByIdWithTenantCheck(
    @Param("id") ID id,
    @Param("tenantId") UUID tenantId,
    @Param("isSuperAdmin") boolean isSuperAdmin
);
```

## Entity Design

All tenant-owned entities extend `TenantAwareEntity`:
```java
@MappedSuperclass
@Getter @Setter
public abstract class TenantAwareEntity extends BaseEntity {
    @Column(name = "tenant_id", nullable = false)
    private UUID tenantId;
}
```

**Tables with tenant_id**: `users`, `roles` (custom), `audit_logs`, `password_reset_tokens`

**Tables WITHOUT tenant_id**: `tenants`, `permissions`, `roles` (system), `spring_session`

## Migration Strategy

Flyway migrations enforce NOT NULL tenant_id:
```sql
-- V2: users table
tenant_id UUID NOT NULL REFERENCES tenants(id) ON DELETE CASCADE

-- V3: roles table (nullable for system roles)
tenant_id UUID REFERENCES tenants(id) ON DELETE CASCADE
```

Unique constraints are composite with tenant_id:
```sql
CREATE UNIQUE INDEX idx_users_tenant_email ON users(tenant_id, email);
```

## Testing Isolation

Integration tests verify cross-tenant access returns 403:
```java
@Test
void crossTenantAccess_shouldBeDenied() {
    // Login as user in tenant1
    String session = loginAs(user1);
    
    // Try to access user2 in tenant2
    mockMvc.perform(get("/api/users/" + user2.getId())
        .header("Cookie", session))
        .andExpect(status().isForbidden());
}
```

## Consequences

### Positive
- Defense in depth: 5 independent layers
- Clear audit trail (which layer blocked access)
- Testable at each layer
- Super Admin bypass for platform operations

### Negative
- Verbosity: tenant_id passed through all layers
- ThreadLocal cleanup critical (implemented in filter `finally` block)
- RLS not yet implemented (Layer 5 is future work)

## References
- [Multi-tenancy Patterns](https://martinfowler.com/articles/multi-tenancy.html)
- [PostgreSQL Row Level Security](https://www.postgresql.org/docs/current/ddl-rowsecurity.html)
- [Spring Security Method Security](https://docs.spring.io/spring-security/reference/servlet/authorization/method-security.html)