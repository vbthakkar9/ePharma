package com.pharmacy.saas.user.service;

import com.pharmacy.saas.common.exception.DuplicateResourceException;
import com.pharmacy.saas.common.exception.ResourceNotFoundException;
import com.pharmacy.saas.common.util.TenantContextHolder;
import com.pharmacy.saas.role.entity.Role;
import com.pharmacy.saas.role.repository.RoleRepository;
import com.pharmacy.saas.user.entity.User;
import com.pharmacy.saas.user.entity.UserStatus;
import com.pharmacy.saas.user.repository.UserRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;

import java.util.Set;
import java.util.UUID;
import java.util.stream.Collectors;

@Service
@RequiredArgsConstructor
public class UserValidationService {

    private final UserRepository userRepository;
    private final RoleRepository roleRepository;

    public User getUserOrThrow(UUID id) {
        UUID tenantId = TenantContextHolder.getRequiredTenantId();
        return userRepository.findByIdAndTenantIdWithRoles(id, tenantId)
                .orElseThrow(() -> new ResourceNotFoundException("User not found with id: " + id));
    }

    public User getUserOrThrow(UUID id, UUID tenantId) {
        return userRepository.findByIdAndTenantIdWithRoles(id, tenantId)
                .orElseThrow(() -> new ResourceNotFoundException("User not found with id: " + id));
    }

    public void validateEmailUnique(String email, UUID tenantId, UUID excludeId) {
        if (userRepository.existsByTenantIdAndEmail(tenantId, email.toLowerCase())) {
            User existing = userRepository.findByTenantIdAndEmail(tenantId, email.toLowerCase()).orElse(null);
            if (existing != null && !existing.getId().equals(excludeId)) {
                throw new DuplicateResourceException("Email already exists in this tenant: " + email);
            }
        }
    }

    public void validateRolesExist(Set<UUID> roleIds, UUID tenantId) {
        if (roleIds == null || roleIds.isEmpty()) {
            return;
        }

        Set<UUID> foundRoleIds = roleRepository.findAllById(roleIds).stream()
                .map(Role::getId)
                .collect(Collectors.toSet());

        Set<UUID> missing = roleIds.stream()
                .filter(id -> !foundRoleIds.contains(id))
                .collect(Collectors.toSet());

        if (!missing.isEmpty()) {
            throw new ResourceNotFoundException("Roles not found: " + missing);
        }

        // Validate all roles belong to this tenant or are system roles
        Set<UUID> invalidTenantRoles = roleRepository.findAllById(roleIds).stream()
                .filter(r -> r.getTenantId() != null && !r.getTenantId().equals(tenantId))
                .map(Role::getId)
                .collect(Collectors.toSet());

        if (!invalidTenantRoles.isEmpty()) {
            throw new IllegalArgumentException("Roles not accessible in this tenant: " + invalidTenantRoles);
        }
    }

    public void validateStatusTransition(UserStatus current, UserStatus target) {
        if (current == target) {
            throw new IllegalArgumentException("User is already in status: " + target);
        }

        // Valid transitions
        boolean valid = switch (current) {
            case PENDING_VERIFICATION -> target == UserStatus.ACTIVE || target == UserStatus.INACTIVE;
            case ACTIVE -> target == UserStatus.INACTIVE || target == UserStatus.LOCKED;
            case INACTIVE -> target == UserStatus.ACTIVE;
            case LOCKED -> target == UserStatus.ACTIVE || target == UserStatus.INACTIVE;
        };

        if (!valid) {
            throw new IllegalArgumentException("Invalid status transition from " + current + " to " + target);
        }
    }

    public void validateNotSelf(UUID userId) {
        UUID currentUserId = TenantContextHolder.getUserId();
        if (currentUserId != null && currentUserId.equals(userId)) {
            throw new IllegalArgumentException("Cannot perform this action on yourself");
        }
    }
}