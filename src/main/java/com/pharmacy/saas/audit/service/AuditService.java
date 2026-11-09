package com.pharmacy.saas.audit.service;

import com.pharmacy.saas.audit.dto.AuditLogResponse;
import com.pharmacy.saas.audit.entity.AuditLog;
import com.pharmacy.saas.audit.repository.AuditLogRepository;
import com.pharmacy.saas.common.response.PageResponse;
import com.pharmacy.saas.common.util.TenantContextHolder;
import lombok.RequiredArgsConstructor;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.Instant;
import java.util.List;
import java.util.UUID;

@Service
@RequiredArgsConstructor
public class AuditService {

    private final AuditLogRepository auditLogRepository;

    @Transactional(readOnly = true)
    public PageResponse<AuditLogResponse> searchAuditLogs(
            String action,
            String entityType,
            UUID userId,
            Instant startDate,
            Instant endDate,
            Pageable pageable
    ) {
        UUID tenantId = TenantContextHolder.getRequiredTenantId();

        Page<AuditLog> logs = auditLogRepository.findByCriteria(
                tenantId, action, entityType, userId, startDate, endDate, pageable
        );

        return PageResponse.from(logs.map(this::toResponse));
    }

    @Transactional(readOnly = true)
    public List<String> getDistinctActions() {
        UUID tenantId = TenantContextHolder.getRequiredTenantId();
        return auditLogRepository.findDistinctActionsByTenant(tenantId);
    }

    @Transactional(readOnly = true)
    public List<String> getDistinctEntityTypes() {
        UUID tenantId = TenantContextHolder.getRequiredTenantId();
        return auditLogRepository.findDistinctEntityTypesByTenant(tenantId);
    }

    private AuditLogResponse toResponse(AuditLog log) {
        return AuditLogResponse.builder()
                .id(log.getId())
                .tenantId(log.getTenantId())
                .userId(log.getUserId())
                .action(log.getAction())
                .entityType(log.getEntityType())
                .entityId(log.getEntityId())
                .metadata(log.getMetadata())
                .ipAddress(log.getIpAddress())
                .userAgent(log.getUserAgent())
                .createdAt(log.getCreatedAt())
                .build();
    }
}