package com.pharmacy.saas.tenant.service;

import com.pharmacy.saas.common.exception.DuplicateResourceException;
import com.pharmacy.saas.common.exception.ResourceNotFoundException;
import com.pharmacy.saas.tenant.entity.Tenant;
import com.pharmacy.saas.tenant.repository.TenantRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;

import java.util.UUID;

@Service
@RequiredArgsConstructor
public class TenantValidationService {

    private final TenantRepository tenantRepository;

    public Tenant getTenantOrThrow(UUID id) {
        return tenantRepository.findById(id)
                .orElseThrow(() -> new ResourceNotFoundException("Tenant not found with id: " + id));
    }

    public void validateTenantCodeUnique(String tenantCode, UUID excludeId) {
        if (tenantRepository.existsByTenantCode(tenantCode)) {
            Tenant existing = tenantRepository.findByTenantCode(tenantCode).orElse(null);
            if (existing != null && !existing.getId().equals(excludeId)) {
                throw new DuplicateResourceException("Tenant code already exists: " + tenantCode);
            }
        }
    }

    public void validateEmailUnique(String email, UUID excludeId) {
        if (tenantRepository.existsByEmail(email)) {
            Tenant existing = tenantRepository.findByEmail(email).orElse(null);
            if (existing != null && !existing.getId().equals(excludeId)) {
                throw new DuplicateResourceException("Email already exists: " + email);
            }
        }
    }

    public void validateStatusTransition(Tenant.TenantStatus current, Tenant.TenantStatus target) {
        if (current == target) {
            throw new IllegalArgumentException("Tenant is already in status: " + target);
        }

        // Valid transitions
        boolean valid = switch (current) {
            case TRIAL -> target == Tenant.TenantStatus.ACTIVE || target == Tenant.TenantStatus.INACTIVE;
            case ACTIVE -> target == Tenant.TenantStatus.INACTIVE || target == Tenant.TenantStatus.SUSPENDED;
            case INACTIVE -> target == Tenant.TenantStatus.ACTIVE;
            case SUSPENDED -> target == Tenant.TenantStatus.ACTIVE || target == Tenant.TenantStatus.INACTIVE;
        };

        if (!valid) {
            throw new IllegalArgumentException("Invalid status transition from " + current + " to " + target);
        }
    }
}