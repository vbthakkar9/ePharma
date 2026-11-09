package com.pharmacy.saas.dashboard.service;

import com.pharmacy.saas.common.util.TenantContextHolder;
import com.pharmacy.saas.dashboard.dto.DashboardStatsResponse;
import com.pharmacy.saas.tenant.entity.Tenant;
import com.pharmacy.saas.tenant.repository.TenantRepository;
import com.pharmacy.saas.user.repository.UserRepository;
import com.pharmacy.saas.user.entity.UserStatus;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.UUID;

@Service
@RequiredArgsConstructor
public class DashboardService {

    private final TenantRepository tenantRepository;
    private final UserRepository userRepository;

    @Transactional(readOnly = true)
    public DashboardStatsResponse getDashboardStats() {
        UUID tenantId = TenantContextHolder.getTenantId();

        if (tenantId != null) {
            // Pharmacy Admin / Pharmacist / Staff view
            return getTenantDashboardStats(tenantId);
        } else {
            // Super Admin view
            return getSuperAdminDashboardStats();
        }
    }

    private DashboardStatsResponse getTenantDashboardStats(UUID tenantId) {
        long totalUsers = userRepository.countByTenantId(tenantId);
        long activeUsers = userRepository.countByTenantIdAndStatus(tenantId, UserStatus.ACTIVE);
        long inactiveUsers = userRepository.countByTenantIdAndStatus(tenantId, UserStatus.INACTIVE);
        long lockedUsers = userRepository.countByTenantIdAndStatus(tenantId, UserStatus.LOCKED);
        long pendingUsers = userRepository.countByTenantIdAndStatus(tenantId, UserStatus.PENDING_VERIFICATION);

        return DashboardStatsResponse.builder()
                .totalUsers(totalUsers)
                .activeUsers(activeUsers)
                .inactiveUsers(inactiveUsers)
                .lockedUsers(lockedUsers)
                .pendingUsers(pendingUsers)
                .build();
    }

    private DashboardStatsResponse getSuperAdminDashboardStats() {
        long totalTenants = tenantRepository.count();
        long activeTenants = tenantRepository.countByStatus(Tenant.TenantStatus.ACTIVE);
        long inactiveTenants = tenantRepository.countByStatus(Tenant.TenantStatus.INACTIVE);
        long suspendedTenants = tenantRepository.countByStatus(Tenant.TenantStatus.SUSPENDED);
        long trialTenants = tenantRepository.countByStatus(Tenant.TenantStatus.TRIAL);
        long totalUsers = userRepository.count();

        return DashboardStatsResponse.builder()
                .totalTenants(totalTenants)
                .activeTenants(activeTenants)
                .inactiveTenants(inactiveTenants)
                .suspendedTenants(suspendedTenants)
                .trialTenants(trialTenants)
                .totalUsers(totalUsers)
                .build();
    }
}