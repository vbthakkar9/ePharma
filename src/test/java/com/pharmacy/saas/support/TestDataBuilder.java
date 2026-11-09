package com.pharmacy.saas.support;

import com.pharmacy.saas.common.util.PasswordUtils;
import com.pharmacy.saas.tenant.entity.Tenant;
import com.pharmacy.saas.user.entity.User;
import com.pharmacy.saas.user.entity.UserStatus;
import com.pharmacy.saas.role.entity.Role;
import com.pharmacy.saas.permission.entity.Permission;
import com.pharmacy.saas.user.entity.UserRole;
import com.pharmacy.saas.role.entity.RolePermission;

import java.time.Instant;
import java.util.HashSet;
import java.util.Set;
import java.util.UUID;

public class TestDataBuilder {

    private static final PasswordUtils.ValidationResult VALID_PASSWORD = new PasswordUtils.ValidationResult();

    static {
        VALID_PASSWORD.valid = true;
    }

    public static Tenant.TenantBuilder aTenant() {
        return Tenant.builder()
                .tenantCode("TEST_" + UUID.randomUUID().toString().substring(0, 8).toUpperCase())
                .pharmacyName("Test Pharmacy")
                .email("test" + UUID.randomUUID().toString().substring(0, 8) + "@example.com")
                .status("ACTIVE")
                .country("India");
    }

    public static User.UserBuilder aUser(UUID tenantId) {
        return User.builder()
                .tenantId(tenantId)
                .firstName("John")
                .lastName("Doe")
                .email("john.doe" + UUID.randomUUID().toString().substring(0, 8) + "@example.com")
                .mobile("+919876543210")
                .passwordHash("hashed_password")
                .status(UserStatus.ACTIVE)
                .pwdChangedAt(Instant.now());
    }

    public static User.UserBuilder aSuperAdmin() {
        return User.builder()
                .tenantId(null)
                .firstName("Super")
                .lastName("Admin")
                .email("superadmin" + UUID.randomUUID().toString().substring(0, 8) + "@example.com")
                .passwordHash("hashed_password")
                .status(UserStatus.ACTIVE)
                .pwdChangedAt(Instant.now());
    }

    public static Role.RoleBuilder aRole(UUID tenantId) {
        return Role.builder()
                .name("CUSTOM_ROLE_" + UUID.randomUUID().toString().substring(0, 8))
                .description("Test custom role")
                .isSystem(false)
                .tenantId(tenantId);
    }

    public static Role.RoleBuilder aSystemRole(String name) {
        return Role.builder()
                .name(name)
                .description("System role: " + name)
                .isSystem(true)
                .tenantId(null);
    }

    public static Permission.PermissionBuilder aPermission() {
        return Permission.builder()
                .name("CUSTOM_PERM_" + UUID.randomUUID().toString().substring(0, 8))
                .description("Test permission");
    }

    public static UserRole.UserRoleBuilder aUserRole(User user, Role role) {
        return UserRole.builder()
                .user(user)
                .role(role)
                .assignedAt(Instant.now());
    }

    public static RolePermission.RolePermissionBuilder aRolePermission(Role role, Permission permission) {
        return RolePermission.builder()
                .role(role)
                .permission(permission)
                .assignedAt(Instant.now());
    }

    public static User createUserWithRoles(UUID tenantId, Set<Role> roles, PasswordUtils passwordUtils) {
        User user = aUser(tenantId)
                .passwordHash(passwordUtils.hashPassword("Test@123", passwordUtils.createDefaultPasswordEncoder()))
                .build();

        Set<UserRole> userRoles = new HashSet<>();
        for (Role role : roles) {
            userRoles.add(aUserRole(user, role).build());
        }
        user.setUserRoles(userRoles);
        return user;
    }
}