package com.pharmacy.saas.permission.service;

import com.pharmacy.saas.common.exception.DuplicateResourceException;
import com.pharmacy.saas.common.exception.ResourceNotFoundException;
import com.pharmacy.saas.common.response.PageResponse;
import com.pharmacy.saas.permission.dto.PermissionResponse;
import com.pharmacy.saas.permission.entity.Permission;
import com.pharmacy.saas.permission.repository.PermissionRepository;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;
import java.util.UUID;
import java.util.stream.Collectors;

@Slf4j
@Service
@RequiredArgsConstructor
public class PermissionService {

    private final PermissionRepository permissionRepository;

    @Transactional(readOnly = true)
    public PermissionResponse getPermission(UUID id) {
        Permission permission = permissionRepository.findById(id)
                .orElseThrow(() -> new ResourceNotFoundException("Permission", id));
        return toResponse(permission);
    }

    @Transactional(readOnly = true)
    public PageResponse<PermissionResponse> searchPermissions(String search, Pageable pageable) {
        Page<Permission> permissions = permissionRepository.search(search, pageable);
        return PageResponse.from(permissions.map(this::toResponse));
    }

    @Transactional(readOnly = true)
    public List<PermissionResponse> getAllPermissions() {
        return permissionRepository.findAll().stream()
                .map(this::toResponse)
                .collect(Collectors.toList());
    }

    @Transactional(readOnly = true)
    public List<PermissionResponse> getPermissionsByIds(Iterable<UUID> ids) {
        return permissionRepository.findAllByIdIn(ids).stream()
                .map(this::toResponse)
                .collect(Collectors.toList());
    }

    @Transactional
    public PermissionResponse createPermission(String name, String description) {
        if (permissionRepository.existsByName(name)) {
            throw new DuplicateResourceException("Permission", "name", name);
        }

        Permission permission = Permission.builder()
                .name(name)
                .description(description)
                .build();

        Permission saved = permissionRepository.save(permission);
        log.info("Created permission: {}", saved.getName());
        return toResponse(saved);
    }

    @Transactional
    public void deletePermission(UUID id) {
        Permission permission = permissionRepository.findById(id)
                .orElseThrow(() -> new ResourceNotFoundException("Permission", id));

        // Check if permission is assigned to any roles
        // This would require checking role_permissions table

        permissionRepository.delete(permission);
        log.info("Deleted permission: {}", id);
    }

    private PermissionResponse toResponse(Permission permission) {
        return PermissionResponse.builder()
                .id(permission.getId())
                .name(permission.getName())
                .description(permission.getDescription())
                .createdAt(permission.getCreatedAt())
                .updatedAt(permission.getUpdatedAt())
                .build();
    }
}