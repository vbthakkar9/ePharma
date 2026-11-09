package com.pharmacy.saas.role.controller;

import com.pharmacy.saas.common.response.ApiResponse;
import com.pharmacy.saas.common.response.PageResponse;
import com.pharmacy.saas.role.dto.RoleRequest;
import com.pharmacy.saas.role.dto.RoleResponse;
import com.pharmacy.saas.role.service.RoleService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.security.SecurityRequirement;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.data.domain.Pageable;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.*;

import java.util.UUID;

@Tag(name = "Role Management", description = "Manage roles and permissions")
@SecurityRequirement(name = "sessionAuth")
@RestController
@RequestMapping("/roles")
@RequiredArgsConstructor
public class RoleController {

    private final RoleService roleService;

    @Operation(summary = "Create a new role")
    @PostMapping
    @PreAuthorize("hasRole('SUPER_ADMIN')")
    public ResponseEntity<ApiResponse<RoleResponse>> createRole(@Valid @RequestBody RoleRequest request) {
        RoleResponse response = roleService.createRole(request);
        return ResponseEntity.ok(ApiResponse.success(response, "Role created successfully"));
    }

    @Operation(summary = "Get role by ID")
    @GetMapping("/{id}")
    @PreAuthorize("hasAnyRole('SUPER_ADMIN', 'PHARMACY_ADMIN')")
    public ResponseEntity<ApiResponse<RoleResponse>> getRole(@PathVariable UUID id) {
        RoleResponse response = roleService.getRole(id);
        return ResponseEntity.ok(ApiResponse.success(response));
    }

    @Operation(summary = "Search roles with pagination")
    @GetMapping
    @PreAuthorize("hasAnyRole('SUPER_ADMIN', 'PHARMACY_ADMIN')")
    public ResponseEntity<ApiResponse<PageResponse<RoleResponse>>> searchRoles(
            @RequestParam(required = false) String search,
            @RequestParam(defaultValue = "0") int page,
            @RequestParam(defaultValue = "20") int size,
            @RequestParam(defaultValue = "name,ASC") String sort
    ) {
        org.springframework.data.domain.Pageable pageable = buildPageable(page, size, sort);
        PageResponse<RoleResponse> response = roleService.searchRoles(search, pageable);
        return ResponseEntity.ok(ApiResponse.success(response));
    }

    @Operation(summary = "Update role")
    @PutMapping("/{id}")
    @PreAuthorize("hasRole('SUPER_ADMIN')")
    public ResponseEntity<ApiResponse<RoleResponse>> updateRole(
            @PathVariable UUID id,
            @Valid @RequestBody RoleRequest request
    ) {
        RoleResponse response = roleService.updateRole(id, request);
        return ResponseEntity.ok(ApiResponse.success(response, "Role updated successfully"));
    }

    @Operation(summary = "Delete role")
    @DeleteMapping("/{id}")
    @PreAuthorize("hasRole('SUPER_ADMIN')")
    public ResponseEntity<ApiResponse<Void>> deleteRole(@PathVariable UUID id) {
        roleService.deleteRole(id);
        return ResponseEntity.ok(ApiResponse.success(null, "Role deleted successfully"));
    }

    private org.springframework.data.domain.Pageable buildPageable(int page, int size, String sort) {
        page = Math.max(0, page);
        size = Math.min(100, Math.max(1, size));

        org.springframework.data.domain.Sort springSort = org.springframework.data.domain.Sort.by(org.springframework.data.domain.Sort.Direction.ASC, "name");
        if (sort != null) {
            String[] parts = sort.split(",");
            if (parts.length == 2) {
                org.springframework.data.domain.Sort.Direction direction = "ASC".equalsIgnoreCase(parts[1]) ? org.springframework.data.domain.Sort.Direction.ASC : org.springframework.data.domain.Sort.Direction.DESC;
                springSort = org.springframework.data.domain.Sort.by(direction, parts[0]);
            }
        }

        return org.springframework.data.domain.PageRequest.of(page, size, springSort);
    }
}