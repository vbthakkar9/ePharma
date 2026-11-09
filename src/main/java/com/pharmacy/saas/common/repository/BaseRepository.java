package com.pharmacy.saas.common.repository;

import com.pharmacy.saas.common.entity.BaseEntity;
import com.pharmacy.saas.common.entity.TenantAwareEntity;
import com.pharmacy.saas.common.util.TenantContextHolder;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.NoRepositoryBean;
import org.springframework.data.repository.query.Param;

import java.util.List;
import java.util.Optional;
import java.util.UUID;

@NoRepositoryBean
public interface BaseRepository<T extends BaseEntity, ID> extends JpaRepository<T, ID> {

    @Query("SELECT e FROM #{#entityName} e WHERE e.tenantId = :tenantId")
    List<T> findAllByTenantId(@Param("tenantId") UUID tenantId);

    @Query("SELECT e FROM #{#entityName} e WHERE e.tenantId = :tenantId")
    Page<T> findAllByTenantId(@Param("tenantId") UUID tenantId, Pageable pageable);

    @Query("SELECT e FROM #{#entityName} e WHERE e.id = :id AND e.tenantId = :tenantId")
    Optional<T> findByIdAndTenantId(@Param("id") ID id, @Param("tenantId") UUID tenantId);

    @Query("SELECT CASE WHEN COUNT(e) > 0 THEN true ELSE false END FROM #{#entityName} e WHERE e.id = :id AND e.tenantId = :tenantId")
    boolean existsByIdAndTenantId(@Param("id") ID id, @Param("tenantId") UUID tenantId);

    @Query("SELECT CASE WHEN COUNT(e) > 0 THEN true ELSE false END FROM #{#entityName} e WHERE e.tenantId = :tenantId")
    boolean existsByTenantId(@Param("tenantId") UUID tenantId);

    default UUID getCurrentTenantId() {
        if (TenantContextHolder.isSuperAdmin()) {
            return null;
        }
        return TenantContextHolder.getTenantId();
    }

    default List<T> findAllByTenantId() {
        UUID tenantId = getCurrentTenantId();
        if (tenantId == null) {
            return findAll();
        }
        return findAllByTenantId(tenantId);
    }

    default Page<T> findAllByTenantId(Pageable pageable) {
        UUID tenantId = getCurrentTenantId();
        if (tenantId == null) {
            return findAll(pageable);
        }
        return findAllByTenantId(tenantId, pageable);
    }

    default Optional<T> findByIdAndTenantId(ID id) {
        UUID tenantId = getCurrentTenantId();
        if (tenantId == null) {
            return findById(id);
        }
        return findByIdAndTenantId(id, tenantId);
    }

    default boolean existsByIdAndTenantId(ID id) {
        UUID tenantId = getCurrentTenantId();
        if (tenantId == null) {
            return existsById(id);
        }
        return existsByIdAndTenantId(id, tenantId);
    }
}