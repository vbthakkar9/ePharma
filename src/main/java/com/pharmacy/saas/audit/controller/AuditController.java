package com.pharmacy.saas.audit.controller;

import com.pharmacy.saas.audit.dto.AuditLogResponse;
import com.pharmacy.saas.audit.service.AuditService;
import com.pharmacy.saas.common.response.ApiResponse;
import com.pharmacy.saas.common.response.PageResponse;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.security.SecurityRequirement;
import io.swagger.v3.oas.annotations.tags.Tag;
import lombok.RequiredArgsConstructor;
import org.springframework.data.domain.Pageable;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.*;

import java.time.Instant;
import java.util.List;
import java.util.UUID;

@Tag(name = "Audit Logs", description = "View audit logs (Super Admin only)")
@SecurityRequirement(name = "sessionAuth")
@RestController
@RequestMapping("/audit-logs")
@RequiredArgsConstructor
public class AuditController {

    private final AuditService auditService;

    @Operation(summary = "Search audit logs with filters")
    @GetMapping
    @PreAuthorize("hasRole('SUPER_ADMIN')")
    public ResponseEntity<ApiResponse<PageResponse<AuditLogResponse>>> searchAuditLogs(
            @RequestParam(required = false) String action,
            @RequestParam(required = false) String entityType,
            @RequestParam(required = false) UUID userId,
            @RequestParam(required = false) Instant startDate,
            @RequestParam(required = false) Instant endDate,
            @RequestParam(defaultValue = "0") int page,
            @RequestParam(defaultValue = "20") int size,
            @RequestParam(defaultValue = "createdAt,DESC") String sort
    ) {
        org.springframework.data.domain.Pageable pageable = buildPageable(page, size, sort);

        PageResponse<AuditLogResponse> response = auditService.searchAuditLogs(
                action, entityType, userId, startDate, endDate, pageable
        );

        return ResponseEntity.ok(ApiResponse.success(response));
    }

    @Operation(summary = "Get distinct actions for filtering")
    @GetMapping("/actions")
    @PreAuthorize("hasRole('SUPER_ADMIN')")
    public ResponseEntity<ApiResponse<List<String>>> getDistinctActions() {
        List<String> actions = auditService.getDistinctActions();
        return ResponseEntity.ok(ApiResponse.success(actions));
    }

    @Operation(summary = "Get distinct entity types for filtering")
    @GetMapping("/entity-types")
    @PreAuthorize("hasRole('SUPER_ADMIN')")
    public ResponseEntity<ApiResponse<List<String>>> getDistinctEntityTypes() {
        List<String> entityTypes = auditService.getDistinctEntityTypes();
        return ResponseEntity.ok(ApiResponse.success(entityTypes));
    }

    private org.springframework.data.domain.Pageable buildPageable(int page, int size, String sort) {
        page = Math.max(0, page);
        size = Math.min(100, Math.max(1, size));

        org.springframework.data.domain.Sort springSort = org.springframework.data.domain.Sort.by(org.springframework.data.domain.Sort.Direction.DESC, "createdAt");
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