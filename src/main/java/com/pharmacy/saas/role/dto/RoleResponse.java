package com.pharmacy.saas.role.dto;

import com.pharmacy.saas.permission.dto.PermissionResponse;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.time.Instant;
import java.util.Set;
import java.util.UUID;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class RoleResponse {

    private UUID id;
    private String name;
    private String description;
    private Boolean isSystem;
    private UUID tenantId;
    private Instant createdAt;
    private Instant updatedAt;
    private Set<PermissionResponse> permissions;
}