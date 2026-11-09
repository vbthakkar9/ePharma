package com.pharmacy.saas.config;

import com.pharmacy.saas.permission.entity.Permission;
import com.pharmacy.saas.permission.repository.PermissionRepository;
import com.pharmacy.saas.role.entity.Role;
import com.pharmacy.saas.role.repository.RoleRepository;
import com.pharmacy.saas.tenant.entity.Tenant;
import com.pharmacy.saas.tenant.repository.TenantRepository;
import com.pharmacy.saas.user.entity.User;
import com.pharmacy.saas.user.repository.UserRepository;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.boot.CommandLineRunner;
import org.springframework.core.annotation.Order;
import org.springframework.stereotype.Component;
import org.springframework.transaction.annotation.Transactional;

import java.util.Arrays;
import java.util.List;
import java.util.Optional;
import java.util.UUID;

@Slf4j
@Component
@RequiredArgsConstructor
@Order(1)
public class DataInitializer implements CommandLineRunner {

    private final PermissionRepository permissionRepository;
    private final RoleRepository roleRepository;
    private final UserRepository userRepository;
    private final TenantRepository tenantRepository;
    private final org.springframework.security.crypto.password.PasswordEncoder passwordEncoder;

    @Override
    @Transactional
    public void run(String... args) {
        // Log the generated BCrypt hash for "password" to use in migrations
        String testHash = passwordEncoder.encode("password");
        log.info("Generated BCrypt hash for 'password': {}", testHash);

        seedPermissions();
        seedSystemRoles();
        seedRolePermissions();
        updateSuperAdminPassword();
    }

    private void updateSuperAdminPassword() {
        Optional<User> superAdmin = userRepository.findByEmailWithoutTenantFilter("superadmin@pharmacy.com");
        if (superAdmin.isPresent()) {
            User user = superAdmin.get();
            String newHash = passwordEncoder.encode("password");
            user.setPasswordHash(newHash);
            user.setStatus(com.pharmacy.saas.user.entity.UserStatus.ACTIVE);
            user.setFailedLoginAttempts(0);
            user.setLockedUntil(null);
            user.setPwdChangedAt(java.time.Instant.now());
            userRepository.save(user);
            log.info("Updated super admin password with correct BCrypt hash: {}", newHash);
        } else {
            // Create super admin user if not exists
            Optional<Tenant> superAdminTenant = tenantRepository.findByTenantCode("SUPER_ADMIN");
            if (superAdminTenant.isPresent()) {
                User user = User.builder()
                        .tenantId(superAdminTenant.get().getId())
                        .firstName("Super")
                        .lastName("Admin")
                        .email("superadmin@pharmacy.com")
                        .mobile("+1000000000")
                        .passwordHash(passwordEncoder.encode("password"))
                        .status(com.pharmacy.saas.user.entity.UserStatus.ACTIVE)
                        .build();
                userRepository.save(user);
                log.info("Created super admin user with correct BCrypt hash");
            }
        }
    }

    private void seedPermissions() {
        List<Permission> permissions = Arrays.asList(
                Permission.builder().name("TENANT_CREATE").description("Create new tenants").build(),
                Permission.builder().name("TENANT_READ").description("View tenant details").build(),
                Permission.builder().name("TENANT_UPDATE").description("Update tenant details").build(),
                Permission.builder().name("TENANT_DELETE").description("Delete tenants").build(),
                Permission.builder().name("TENANT_STATUS_CHANGE").description("Change tenant status").build(),
                Permission.builder().name("USER_CREATE").description("Create new users").build(),
                Permission.builder().name("USER_READ").description("View user details").build(),
                Permission.builder().name("USER_UPDATE").description("Update user details").build(),
                Permission.builder().name("USER_DELETE").description("Delete users").build(),
                Permission.builder().name("USER_STATUS_CHANGE").description("Change user status").build(),
                Permission.builder().name("USER_ROLE_ASSIGN").description("Assign roles to users").build(),
                Permission.builder().name("USER_PASSWORD_RESET").description("Reset user passwords").build(),
                Permission.builder().name("ROLE_CREATE").description("Create new roles").build(),
                Permission.builder().name("ROLE_READ").description("View role details").build(),
                Permission.builder().name("ROLE_UPDATE").description("Update role details").build(),
                Permission.builder().name("ROLE_DELETE").description("Delete roles").build(),
                Permission.builder().name("ROLE_PERMISSION_ASSIGN").description("Assign permissions to roles").build(),
                Permission.builder().name("PERMISSION_CREATE").description("Create new permissions").build(),
                Permission.builder().name("PERMISSION_READ").description("View permission details").build(),
                Permission.builder().name("PERMISSION_UPDATE").description("Update permission details").build(),
                Permission.builder().name("PERMISSION_DELETE").description("Delete permissions").build(),
                Permission.builder().name("AUDIT_LOG_READ").description("View audit logs").build(),
                Permission.builder().name("DASHBOARD_VIEW").description("View dashboard statistics").build()
        );

        for (Permission perm : permissions) {
            if (!permissionRepository.existsByName(perm.getName())) {
                permissionRepository.save(perm);
                log.info("Seeded permission: {}", perm.getName());
            }
        }
    }

    private void seedSystemRoles() {
        List<Role> systemRoles = Arrays.asList(
                Role.builder()
                        .name("SUPER_ADMIN")
                        .description("Super Administrator with full system access")
                        .isSystem(true)
                        .tenantId(null)
                        .build(),
                Role.builder()
                        .name("PHARMACY_ADMIN")
                        .description("Pharmacy Administrator with tenant management access")
                        .isSystem(true)
                        .tenantId(null)
                        .build(),
                Role.builder()
                        .name("PHARMACIST")
                        .description("Pharmacist with pharmacy operations access")
                        .isSystem(true)
                        .tenantId(null)
                        .build(),
                Role.builder()
                        .name("PHARMACY_STAFF")
                        .description("Pharmacy Staff with limited access")
                        .isSystem(true)
                        .tenantId(null)
                        .build()
        );

        for (Role role : systemRoles) {
            if (!roleRepository.existsByNameAndTenantId(role.getName(), null)) {
                roleRepository.save(role);
                log.info("Seeded system role: {}", role.getName());
            }
        }
    }

    private void seedRolePermissions() {
        // This is handled by the Flyway migration V5
        // Just log that it's expected to be done via migration
        log.info("Role-permission matrix should be seeded via Flyway migration V5");
    }
}