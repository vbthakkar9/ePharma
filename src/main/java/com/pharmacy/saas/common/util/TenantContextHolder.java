package com.pharmacy.saas.common.util;

import com.pharmacy.saas.common.constants.ApplicationConstants;
import lombok.extern.slf4j.Slf4j;

import java.util.UUID;

@Slf4j
public final class TenantContextHolder {

    private static final ThreadLocal<UUID> tenantContext = new ThreadLocal<>();
    private static final ThreadLocal<Boolean> superAdminContext = new ThreadLocal<>();

    private TenantContextHolder() {}

    public static void setTenantId(UUID tenantId) {
        if (tenantId == null) {
            log.debug("Setting tenant context to NULL (Super Admin)");
            superAdminContext.set(true);
        } else {
            log.debug("Setting tenant context to: {}", tenantId);
            tenantContext.set(tenantId);
            superAdminContext.set(false);
        }
    }

    public static UUID getTenantId() {
        return tenantContext.get();
    }

    public static boolean isSuperAdmin() {
        return Boolean.TRUE.equals(superAdminContext.get());
    }

    public static void clear() {
        tenantContext.remove();
        superAdminContext.remove();
        log.debug("Cleared tenant context");
    }

    public static boolean hasTenantContext() {
        return tenantContext.get() != null || isSuperAdmin();
    }

    public static UUID getRequiredTenantId() {
        UUID tenantId = tenantContext.get();
        if (tenantId == null && !isSuperAdmin()) {
            throw new IllegalStateException("Tenant context not set");
        }
        return tenantId;
    }

    public static void setUserId(UUID userId) {
        // Could add userId context if needed
    }

    public static UUID getUserId() {
        // This would be set from authentication context
        return null;
    }

    public static String getTenantIdHeaderValue() {
        UUID tenantId = getTenantId();
        if (tenantId == null && !isSuperAdmin()) {
            return null;
        }
        return tenantId != null ? tenantId.toString() : ApplicationConstants.SUPER_ADMIN_ROLE;
    }
}