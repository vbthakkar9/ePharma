package com.pharmacy.saas.tenant.repository;

import com.pharmacy.saas.tenant.entity.Tenant;
import com.pharmacy.saas.tenant.dto.TenantSearchCriteria;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.Optional;
import java.util.UUID;

@Repository
public interface TenantRepository extends JpaRepository<Tenant, UUID> {

    Optional<Tenant> findByTenantCode(String tenantCode);

    Optional<Tenant> findByEmail(String email);

    boolean existsByTenantCode(String tenantCode);

    boolean existsByEmail(String email);

    List<Tenant> findByStatus(Tenant.TenantStatus status);

    @Query("SELECT t FROM Tenant t WHERE " +
            "(:search IS NULL OR t.tenantCode ILIKE %:search% OR t.pharmacyName ILIKE %:search% OR t.email ILIKE %:search%) AND " +
            "(:status IS NULL OR t.status = :status)")
    Page<Tenant> search(@Param("search") String search,
                        @Param("status") Tenant.TenantStatus status,
                        Pageable pageable);

    @Query("SELECT COUNT(t) FROM Tenant t WHERE t.status = :status")
    long countByStatus(Tenant.TenantStatus status);
}