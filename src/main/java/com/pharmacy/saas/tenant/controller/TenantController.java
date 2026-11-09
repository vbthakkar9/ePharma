package com.pharmacy.saas.tenant.controller;

import com.pharmacy.saas.common.response.ApiResponse;
import com.pharmacy.saas.common.response.PageResponse;
import com.pharmacy.saas.tenant.dto.TenantRequest;
import com.pharmacy.saas.tenant.dto.TenantResponse;
import com.pharmacy.saas.tenant.dto.TenantSearchCriteria;
import com.pharmacy.saas.tenant.entity.Tenant;
import com.pharmacy.saas.tenant.service.TenantService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.security.SecurityRequirement;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.*;

import java.util.UUID;

@Tag(name = "Tenant Management", description = "Manage tenants (Super Admin only)")
@SecurityRequirement(name = "sessionAuth")
@RestController
@RequestMapping("/tenants")
@RequiredArgsConstructor
public class TenantController {

    private final TenantService tenantService;

    @Operation(summary = "Create a new tenant")
    @PostMapping
    @PreAuthorize("hasRole('SUPER_ADMIN')")
    public ResponseEntity<ApiResponse<TenantResponse>> createTenant(@Valid @RequestBody TenantRequest request) {
        TenantResponse response = tenantService.createTenant(request);
        return ResponseEntity.ok(ApiResponse.success(response, "Tenant created successfully"));
    }

    @Operation(summary = "Get tenant by ID")
    @GetMapping("/{id}")
    @PreAuthorize("hasRole('SUPER_ADMIN')")
    public ResponseEntity<ApiResponse<TenantResponse>> getTenant(@PathVariable UUID id) {
        TenantResponse response = tenantService.getTenant(id);
        return ResponseEntity.ok(ApiResponse.success(response));
    }

    @Operation(summary = "Search tenants with pagination")
    @GetMapping
    @PreAuthorize("hasRole('SUPER_ADMIN')")
    public ResponseEntity<ApiResponse<PageResponse<TenantResponse>>> searchTenants(
            @RequestParam(required = false) String search,
            @RequestParam(required = false) Tenant.TenantStatus status,
            @RequestParam(defaultValue = "0") int page,
            @RequestParam(defaultValue = "20") int size,
            @RequestParam(defaultValue = "createdAt,DESC") String sort
    ) {
        TenantSearchCriteria criteria = TenantSearchCriteria.builder()
                .search(search)
                .status(status)
                .page(page)
                .size(size)
                .sort(sort)
                .build();

        PageResponse<TenantResponse> response = tenantService.searchTenants(criteria);
        return ResponseEntity.ok(ApiResponse.success(response));
    }

    @Operation(summary = "Update tenant")
    @PutMapping("/{id}")
    @PreAuthorize("hasRole('SUPER_ADMIN')")
    public ResponseEntity<ApiResponse<TenantResponse>> updateTenant(
            @PathVariable UUID id,
            @Valid @RequestBody TenantRequest request
    ) {
        TenantResponse response = tenantService.updateTenant(id, request);
        return ResponseEntity.ok(ApiResponse.success(response, "Tenant updated successfully"));
    }

    @Operation(summary = "Change tenant status")
    @PatchMapping("/{id}/status")
    @PreAuthorize("hasRole('SUPER_ADMIN')")
    public ResponseEntity<ApiResponse<TenantResponse>> changeStatus(
            @PathVariable UUID id,
            @RequestParam Tenant.TenantStatus status
    ) {
        TenantResponse response = tenantService.changeStatus(id, status);
        return ResponseEntity.ok(ApiResponse.success(response, "Tenant status changed to " + status));
    }

    @Operation(summary = "Delete tenant")
    @DeleteMapping("/{id}")
    @PreAuthorize("hasRole('SUPER_ADMIN')")
    public ResponseEntity<ApiResponse<Void>> deleteTenant(@PathVariable UUID id) {
        tenantService.deleteTenant(id);
        return ResponseEntity.ok(ApiResponse.success(null, "Tenant deleted successfully"));
    }
}