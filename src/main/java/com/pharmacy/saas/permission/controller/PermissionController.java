package com.pharmacy.saas.permission.controller;

import com.pharmacy.saas.common.response.ApiResponse;
import com.pharmacy.saas.common.response.PageResponse;
import com.pharmacy.saas.permission.dto.PermissionResponse;
import com.pharmacy.saas.permission.service.PermissionService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.security.SecurityRequirement;
import io.swagger.v3.oas.annotations.tags.Tag;
import lombok.RequiredArgsConstructor;
import org.springframework.data.domain.Pageable;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.*;

import java.util.List;
import java.util.UUID;

@Tag(name = "Permission Management", description = "Manage permissions (Super Admin only)")
@SecurityRequirement(name = "sessionAuth")
@RestController
@RequestMapping("/permissions")
@RequiredArgsConstructor
public class PermissionController {

    private final PermissionService permissionService;

    @Operation(summary = "Get all permissions")
    @GetMapping
    @PreAuthorize("hasRole('SUPER_ADMIN')")
    public ResponseEntity<ApiResponse<List<PermissionResponse>>> getAllPermissions() {
        List<PermissionResponse> response = permissionService.getAllPermissions();
        return ResponseEntity.ok(ApiResponse.success(response));
    }

    @Operation(summary = "Get permission by ID")
    @GetMapping("/{id}")
    @PreAuthorize("hasRole('SUPER_ADMIN')")
    public ResponseEntity<ApiResponse<PermissionResponse>> getPermission(@PathVariable UUID id) {
        PermissionResponse response = permissionService.getPermission(id);
        return ResponseEntity.ok(ApiResponse.success(response));
    }

    @Operation(summary = "Search permissions with pagination")
    @GetMapping("/search")
    @PreAuthorize("hasRole('SUPER_ADMIN')")
    public ResponseEntity<ApiResponse<PageResponse<PermissionResponse>>> searchPermissions(
            @RequestParam(required = false) String search,
            @RequestParam(defaultValue = "0") int page,
            @RequestParam(defaultValue = "20") int size,
            @RequestParam(defaultValue = "name,ASC") String sort
    ) {
        org.springframework.data.domain.Pageable pageable = buildPageable(page, size, sort);
        PageResponse<PermissionResponse> response = permissionService.searchPermissions(search, pageable);
        return ResponseEntity.ok(ApiResponse.success(response));
    }

    @Operation(summary = "Create permission")
    @PostMapping
    @PreAuthorize("hasRole('SUPER_ADMIN')")
    public ResponseEntity<ApiResponse<PermissionResponse>> createPermission(
            @RequestParam String name,
            @RequestParam(required = false) String description
    ) {
        PermissionResponse response = permissionService.createPermission(name, description);
        return ResponseEntity.ok(ApiResponse.success(response, "Permission created successfully"));
    }

    @Operation(summary = "Delete permission")
    @DeleteMapping("/{id}")
    @PreAuthorize("hasRole('SUPER_ADMIN')")
    public ResponseEntity<ApiResponse<Void>> deletePermission(@PathVariable UUID id) {
        permissionService.deletePermission(id);
        return ResponseEntity.ok(ApiResponse.success(null, "Permission deleted successfully"));
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