package com.pharmacy.saas.user.dto;

import com.pharmacy.saas.user.entity.User;
import com.pharmacy.saas.user.entity.UserStatus;
import lombok.Builder;
import lombok.Data;

import java.time.Instant;
import java.util.Set;
import java.util.UUID;

@Data
@Builder
public class UserResponse {

    private UUID id;
    private UUID tenantId;
    private String firstName;
    private String lastName;
    private String email;
    private String mobile;
    private String status;
    private Instant lastLoginAt;
    private Instant pwdChangedAt;
    private Instant createdAt;
    private Instant updatedAt;
    private Set<RoleResponse> roles;

    public static UserResponse from(User user) {
        Set<RoleResponse> roleResponses = user.getUserRoles().stream()
                .map(ur -> RoleResponse.builder()
                        .id(ur.getRole().getId())
                        .name(ur.getRole().getName())
                        .description(ur.getRole().getDescription())
                        .isSystem(ur.getRole().getIsSystem())
                        .build())
                .collect(java.util.stream.Collectors.toSet());

        return UserResponse.builder()
                .id(user.getId())
                .tenantId(user.getTenantId())
                .firstName(user.getFirstName())
                .lastName(user.getLastName())
                .email(user.getEmail())
                .mobile(user.getMobile())
                .status(user.getStatus().name())
                .lastLoginAt(user.getLastLoginAt())
                .pwdChangedAt(user.getPwdChangedAt())
                .createdAt(user.getCreatedAt())
                .updatedAt(user.getUpdatedAt())
                .roles(roleResponses)
                .build();
    }
}