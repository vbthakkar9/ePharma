package com.pharmacy.saas.role.repository;

import com.pharmacy.saas.common.repository.TenantAwareRepository;
import com.pharmacy.saas.role.entity.Role;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.Optional;
import java.util.UUID;

@Repository
public interface RoleRepository extends TenantAwareRepository<Role, UUID> {

    Optional<Role> findByNameAndTenantId(String name, UUID tenantId);

    Optional<Role> findByNameAndIsSystemTrue(String name);

    boolean existsByNameAndTenantId(String name, UUID tenantId);

    boolean existsByNameAndIsSystemTrue(String name);

    @Query("SELECT r FROM Role r WHERE r.isSystem = true")
    List<Role> findSystemRoles();

    @Query("""
            SELECT r FROM Role r
            WHERE (r.tenantId = :tenantId OR r.isSystem = true)
            AND (:search IS NULL OR LOWER(r.name) LIKE LOWER(CONCAT('%', :search, '%')))
            ORDER BY r.isSystem DESC, r.name ASC
            """)
    Page<Role> search(@Param("tenantId") UUID tenantId,
                      @Param("search") String search,
                      Pageable pageable);

    @Query("SELECT r FROM Role r LEFT JOIN FETCH r.rolePermissions rp LEFT JOIN FETCH rp.permission WHERE r.id = :id")
    Optional<Role> findByIdWithPermissions(@Param("id") UUID id);
}