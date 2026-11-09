package com.pharmacy.saas.audit.repository;

import com.pharmacy.saas.audit.entity.AuditLog;
import com.pharmacy.saas.common.repository.TenantAwareRepository;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import java.time.Instant;
import java.util.List;
import java.util.Optional;
import java.util.UUID;

@Repository
public interface AuditLogRepository extends TenantAwareRepository<AuditLog, UUID> {

    @Query("""
            SELECT a FROM AuditLog a
            WHERE a.tenantId = :tenantId
            AND (:action IS NULL OR a.action = :action)
            AND (:entityType IS NULL OR a.entityType = :entityType)
            AND (:userId IS NULL OR a.userId = :userId)
            AND (:startDate IS NULL OR a.createdAt >= :startDate)
            AND (:endDate IS NULL OR a.createdAt <= :endDate)
            ORDER BY a.createdAt DESC
            """)
    Page<AuditLog> findByCriteria(
            @Param("tenantId") UUID tenantId,
            @Param("action") String action,
            @Param("entityType") String entityType,
            @Param("userId") UUID userId,
            @Param("startDate") Instant startDate,
            @Param("endDate") Instant endDate,
            Pageable pageable
    );

    @Query("SELECT a FROM AuditLog a WHERE a.tenantId = :tenantId ORDER BY a.createdAt DESC")
    Page<AuditLog> findByTenantIdOrderByCreatedAtDesc(UUID tenantId, Pageable pageable);

    @Query("SELECT a FROM AuditLog a WHERE a.userId = :userId ORDER BY a.createdAt DESC")
    Page<AuditLog> findByUserIdOrderByCreatedAtDesc(UUID userId, Pageable pageable);

    @Query("SELECT DISTINCT a.action FROM AuditLog a WHERE a.tenantId = :tenantId ORDER BY a.action")
    List<String> findDistinctActionsByTenant(UUID tenantId);

    @Query("SELECT DISTINCT a.entityType FROM AuditLog a WHERE a.tenantId = :tenantId ORDER BY a.entityType")
    List<String> findDistinctEntityTypesByTenant(UUID tenantId);
}