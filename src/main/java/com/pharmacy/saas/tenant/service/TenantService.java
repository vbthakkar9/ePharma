package com.pharmacy.saas.tenant.service;

import com.pharmacy.saas.common.exception.BusinessException;
import com.pharmacy.saas.common.response.PageResponse;
import com.pharmacy.saas.tenant.dto.TenantRequest;
import com.pharmacy.saas.tenant.dto.TenantResponse;
import com.pharmacy.saas.tenant.dto.TenantSearchCriteria;
import com.pharmacy.saas.tenant.entity.Tenant;
import com.pharmacy.saas.tenant.repository.TenantRepository;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Pageable;
import org.springframework.data.domain.Sort;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.UUID;

@Slf4j
@Service
@RequiredArgsConstructor
public class TenantService {

    private final TenantRepository tenantRepository;
    private final TenantValidationService validationService;

    @Transactional
    public TenantResponse createTenant(TenantRequest request) {
        validationService.validateTenantCodeUnique(request.getTenantCode(), null);
        validationService.validateEmailUnique(request.getEmail(), null);

        Tenant tenant = Tenant.builder()
                .tenantCode(request.getTenantCode().toUpperCase())
                .pharmacyName(request.getPharmacyName())
                .email(request.getEmail().toLowerCase())
                .mobile(request.getMobile())
                .address(request.getAddress())
                .city(request.getCity())
                .state(request.getState())
                .country(request.getCountry() != null ? request.getCountry() : "India")
                .postalCode(request.getPostalCode())
                .status(Tenant.TenantStatus.TRIAL)
                .build();

        Tenant saved = tenantRepository.save(tenant);
        log.info("Created tenant: {} ({})", saved.getPharmacyName(), saved.getTenantCode());
        return TenantResponse.from(saved);
    }

    @Transactional(readOnly = true)
    public TenantResponse getTenant(UUID id) {
        Tenant tenant = validationService.getTenantOrThrow(id);
        return TenantResponse.from(tenant);
    }

    @Transactional(readOnly = true)
    public PageResponse<TenantResponse> searchTenants(TenantSearchCriteria criteria) {
        Pageable pageable = buildPageable(criteria);
        Page<Tenant> tenants = tenantRepository.search(criteria.getSearch(), criteria.getStatus(), pageable);
        return PageResponse.from(tenants.map(TenantResponse::from));
    }

    @Transactional
    public TenantResponse updateTenant(UUID id, TenantRequest request) {
        Tenant tenant = validationService.getTenantOrThrow(id);

        validationService.validateTenantCodeUnique(request.getTenantCode(), id);
        validationService.validateEmailUnique(request.getEmail(), id);

        tenant.setTenantCode(request.getTenantCode().toUpperCase());
        tenant.setPharmacyName(request.getPharmacyName());
        tenant.setEmail(request.getEmail().toLowerCase());
        tenant.setMobile(request.getMobile());
        tenant.setAddress(request.getAddress());
        tenant.setCity(request.getCity());
        tenant.setState(request.getState());
        tenant.setCountry(request.getCountry());
        tenant.setPostalCode(request.getPostalCode());

        Tenant saved = tenantRepository.save(tenant);
        log.info("Updated tenant: {}", id);
        return TenantResponse.from(saved);
    }

    @Transactional
    public TenantResponse changeStatus(UUID id, Tenant.TenantStatus newStatus) {
        Tenant tenant = validationService.getTenantOrThrow(id);
        validationService.validateStatusTransition(tenant.getStatus(), newStatus);

        Tenant.TenantStatus oldStatus = tenant.getStatus();
        tenant.setStatus(newStatus);

        Tenant saved = tenantRepository.save(tenant);
        log.info("Changed tenant {} status from {} to {}", id, oldStatus, newStatus);
        return TenantResponse.from(saved);
    }

    @Transactional
    public void deleteTenant(UUID id) {
        Tenant tenant = validationService.getTenantOrThrow(id);

        if (tenant.getStatus() == Tenant.TenantStatus.ACTIVE) {
            throw new BusinessException("CANNOT_DELETE_ACTIVE_TENANT", "Cannot delete an active tenant. Deactivate first.");
        }

        tenantRepository.delete(tenant);
        log.info("Deleted tenant: {}", id);
    }

    private Pageable buildPageable(TenantSearchCriteria criteria) {
        int page = Math.max(0, criteria.getPage());
        int size = Math.min(100, Math.max(1, criteria.getSize()));

        Sort sort = Sort.by(Sort.Direction.DESC, "createdAt");
        if (criteria.getSort() != null) {
            String[] parts = criteria.getSort().split(",");
            if (parts.length == 2) {
                Sort.Direction direction = "ASC".equalsIgnoreCase(parts[1]) ? Sort.Direction.ASC : Sort.Direction.DESC;
                sort = Sort.by(direction, parts[0]);
            }
        }

        return PageRequest.of(page, size, sort);
    }
}