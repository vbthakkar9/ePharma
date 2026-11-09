package com.pharmacy.saas.dashboard.dto;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class DashboardStatsResponse {

    // Tenant-level stats
    private Long totalUsers;
    private Long activeUsers;
    private Long inactiveUsers;
    private Long lockedUsers;
    private Long pendingUsers;

    // Super Admin stats
    private Long totalTenants;
    private Long activeTenants;
    private Long inactiveTenants;
    private Long suspendedTenants;
    private Long trialTenants;
}