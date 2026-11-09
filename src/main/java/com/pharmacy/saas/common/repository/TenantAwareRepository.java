package com.pharmacy.saas.common.repository;

import com.pharmacy.saas.common.util.TenantContextHolder;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.NoRepositoryBean;
import org.springframework.data.repository.query.Param;

import java.util.List;
import java.util.Optional;
import java.util.UUID;

@NoRepositoryBean
public interface TenantAwareRepository<T, ID> extends JpaRepository<T, ID> {

    @Query("SELECT e FROM #{#entityName} e WHERE e.tenantId = :tenantId")
    List<T> findAllByTenantId(@Param("tenantId") UUID tenantId);

    @Query("SELECT e FROM #{#entityName} e WHERE e.id = :id AND e.tenantId = :tenantId")
    Optional<T> findByIdAndTenantId(@Param("id") ID id, @Param("tenantId") UUID tenantId);

    @Query("SELECT COUNT(e) FROM #{#entityName} e WHERE e.tenantId = :tenantId")
    long countByTenantId(@Param("tenantId") UUID tenantId);

    @Query("SELECT CASE WHEN COUNT(e) > 0 THEN true ELSE false END FROM #{#entityName} e WHERE e.id = :id AND e.tenantId = :tenantId")
    boolean existsByIdAndTenantId(@Param("id") ID id, @Param("tenantId") UUID tenantId);

    default Optional<T> findByIdCurrentTenant(ID id) {
        UUID tenantId = TenantContextHolder.getTenantId();
        if (tenantId == null) {
            return Optional.empty();
        }
        return findByIdAndTenantId(id, tenantId);
    }

    default boolean existsByIdCurrentTenant(ID id) {
        UUID tenantId = TenantContextHolder.getTenantId();
        if (tenantId == null) {
            return false;
        }
        return existsByIdAndTenantId(id, tenantId);
    }
}