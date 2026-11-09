package com.pharmacy.saas.user.repository;

import com.pharmacy.saas.common.repository.TenantAwareRepository;
import com.pharmacy.saas.user.dto.UserSearchCriteria;
import com.pharmacy.saas.user.entity.User;
import com.pharmacy.saas.user.entity.UserStatus;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.Optional;
import java.util.UUID;

@Repository
public interface UserRepository extends TenantAwareRepository<User, UUID> {

    Optional<User> findByTenantIdAndEmail(UUID tenantId, String email);

    Optional<User> findByEmailAndTenantId(String email, UUID tenantId);

    @Query("SELECT u FROM User u WHERE u.email = :email")
    Optional<User> findByEmailWithoutTenantFilter(String email);

    boolean existsByTenantIdAndEmail(UUID tenantId, String email);

    List<User> findByTenantIdAndStatus(UUID tenantId, UserStatus status);

    long countByTenantIdAndStatus(UUID tenantId, UserStatus status);

    @Query("SELECT u FROM User u LEFT JOIN FETCH u.userRoles ur LEFT JOIN FETCH ur.role " +
            "WHERE u.tenantId = :tenantId AND " +
            "(:search IS NULL OR u.firstName ILIKE %:search% OR u.lastName ILIKE %:search% OR u.email ILIKE %:search% OR u.mobile ILIKE %:search%) AND " +
            "(:status IS NULL OR u.status = :status) AND " +
            "(:roleId IS NULL OR EXISTS (SELECT 1 FROM u.userRoles ur2 WHERE ur2.role.id = :roleId))")
    Page<User> search(@Param("tenantId") UUID tenantId,
                      @Param("search") String search,
                      @Param("status") UserStatus status,
                      @Param("roleId") UUID roleId,
                      Pageable pageable);

    @Query("SELECT u FROM User u LEFT JOIN FETCH u.userRoles ur LEFT JOIN FETCH ur.role WHERE u.id = :id AND u.tenantId = :tenantId")
    Optional<User> findByIdAndTenantIdWithRoles(@Param("id") UUID id, @Param("tenantId") UUID tenantId);
}