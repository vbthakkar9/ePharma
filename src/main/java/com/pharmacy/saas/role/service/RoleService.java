package com.pharmacy.saas.role.service;

import com.pharmacy.saas.common.exception.BusinessException;
import com.pharmacy.saas.common.exception.DuplicateResourceException;
import com.pharmacy.saas.common.exception.ResourceNotFoundException;
import com.pharmacy.saas.common.response.PageResponse;
import com.pharmacy.saas.common.util.TenantContextHolder;
import com.pharmacy.saas.permission.dto.PermissionResponse;
import com.pharmacy.saas.permission.entity.Permission;
import com.pharmacy.saas.permission.repository.PermissionRepository;
import com.pharmacy.saas.role.dto.RoleRequest;
import com.pharmacy.saas.role.dto.RoleResponse;
import com.pharmacy.saas.role.entity.Role;
import com.pharmacy.saas.role.entity.RolePermission;
import com.pharmacy.saas.role.repository.RoleRepository;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.Instant;
import java.util.Set;
import java.util.UUID;
import java.util.stream.Collectors;

@Slf4j
@Service
@RequiredArgsConstructor
public class RoleService {

    private final RoleRepository roleRepository;
    private final PermissionRepository permissionRepository;

    @Transactional
    public RoleResponse createRole(RoleRequest request) {
        UUID tenantId = TenantContextHolder.getRequiredTenantId();

        if (roleRepository.existsByNameAndTenantId(request.getName(), tenantId)) {
            throw new DuplicateResourceException("Role", "name", request.getName());
        }

        Role role = Role.builder()
                .name(request.getName())
                .description(request.getDescription())
                .isSystem(false)
                .tenantId(tenantId)
                .build();

        // Assign permissions if provided
        if (request.getPermissionIds() != null && !request.getPermissionIds().isEmpty()) {
            Set<Permission> permissions = permissionRepository.findAllById(request.getPermissionIds())
                    .stream()
                    .collect(Collectors.toSet());
            assignPermissionsToRole(role, permissions);
        }

        Role saved = roleRepository.save(role);
        log.info("Created role: {} in tenant {}", saved.getName(), tenantId);
        return toResponse(saved);
    }

    @Transactional(readOnly = true)
    public RoleResponse getRole(UUID id) {
        Role role = roleRepository.findByIdWithPermissions(id)
                .orElseThrow(() -> new ResourceNotFoundException("Role", id));

        // Verify tenant access
        UUID tenantId = TenantContextHolder.getTenantId();
        if (tenantId != null && !role.getIsSystem() && !role.getTenantId().equals(tenantId)) {
            throw new ResourceNotFoundException("Role", id);
        }

        return toResponse(role);
    }

    @Transactional(readOnly = true)
    public PageResponse<RoleResponse> searchRoles(String search, Pageable pageable) {
        UUID tenantId = TenantContextHolder.getRequiredTenantId();
        Page<Role> roles = roleRepository.search(tenantId, search, pageable);
        return PageResponse.from(roles.map(this::toResponse));
    }

    @Transactional
    public RoleResponse updateRole(UUID id, RoleRequest request) {
        Role role = roleRepository.findByIdWithPermissions(id)
                .orElseThrow(() -> new ResourceNotFoundException("Role", id));

        // Cannot modify system roles
        if (role.getIsSystem()) {
            throw new BusinessException("ROLE_IS_SYSTEM", "Cannot modify system roles");
        }

        // Verify tenant access
        UUID tenantId = TenantContextHolder.getRequiredTenantId();
        if (!role.getTenantId().equals(tenantId)) {
            throw new ResourceNotFoundException("Role", id);
        }

        if (roleRepository.existsByNameAndTenantId(request.getName(), tenantId)) {
            Role existing = roleRepository.findByNameAndTenantId(request.getName(), tenantId).orElse(null);
            if (existing != null && !existing.getId().equals(id)) {
                throw new DuplicateResourceException("Role", "name", request.getName());
            }
        }

        role.setName(request.getName());
        role.setDescription(request.getDescription());

        // Update permissions if provided
        if (request.getPermissionIds() != null) {
            role.getRolePermissions().clear();
            if (!request.getPermissionIds().isEmpty()) {
                Set<Permission> permissions = permissionRepository.findAllById(request.getPermissionIds())
                        .stream()
                        .collect(Collectors.toSet());
                assignPermissionsToRole(role, permissions);
            }
        }

        Role saved = roleRepository.save(role);
        log.info("Updated role: {}", saved.getId());
        return toResponse(saved);
    }

    @Transactional
    public void deleteRole(UUID id) {
        Role role = roleRepository.findById(id)
                .orElseThrow(() -> new ResourceNotFoundException("Role", id));

        // Cannot delete system roles
        if (role.getIsSystem()) {
            throw new BusinessException("ROLE_IS_SYSTEM", "Cannot delete system roles");
        }

        // Verify tenant access
        UUID tenantId = TenantContextHolder.getRequiredTenantId();
        if (!role.getTenantId().equals(tenantId)) {
            throw new ResourceNotFoundException("Role", id);
        }

        // Check if role is assigned to any users
        // This would require a query to user_roles table

        roleRepository.delete(role);
        log.info("Deleted role: {}", id);
    }

    private void assignPermissionsToRole(Role role, Set<Permission> permissions) {
        for (Permission permission : permissions) {
            RolePermission rp = RolePermission.builder()
                    .role(role)
                    .permission(permission)
                    .assignedAt(Instant.now())
                    .build();
            role.getRolePermissions().add(rp);
        }
    }

    private RoleResponse toResponse(Role role) {
        Set<PermissionResponse> permissionResponses = role.getRolePermissions().stream()
                .map(rp -> PermissionResponse.builder()
                        .id(rp.getPermission().getId())
                        .name(rp.getPermission().getName())
                        .description(rp.getPermission().getDescription())
                        .build())
                .collect(Collectors.toSet());

        return RoleResponse.builder()
                .id(role.getId())
                .name(role.getName())
                .description(role.getDescription())
                .isSystem(role.getIsSystem())
                .tenantId(role.getTenantId())
                .createdAt(role.getCreatedAt())
                .updatedAt(role.getUpdatedAt())
                .permissions(permissionResponses)
                .build();
    }
}