package com.pharmacy.saas.security.filter;

import com.pharmacy.saas.common.util.TenantContextHolder;
import jakarta.servlet.FilterChain;
import jakarta.servlet.ServletException;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import lombok.extern.slf4j.Slf4j;
import org.springframework.lang.NonNull;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.stereotype.Component;
import org.springframework.web.filter.OncePerRequestFilter;

import java.io.IOException;
import java.util.UUID;

@Slf4j
@Component
public class TenantContextFilter extends OncePerRequestFilter {

    private static final String SUPER_ADMIN_ROLE = "ROLE_SUPER_ADMIN";

    @Override
    protected void doFilterInternal(
            @NonNull HttpServletRequest request,
            @NonNull HttpServletResponse response,
            @NonNull FilterChain filterChain
    ) throws ServletException, IOException {

        try {
            Authentication authentication = SecurityContextHolder.getContext().getAuthentication();

            if (authentication != null && authentication.isAuthenticated()
                    && !"anonymousUser".equals(authentication.getPrincipal())) {

                boolean isSuperAdmin = authentication.getAuthorities().stream()
                        .anyMatch(a -> a.getAuthority().equals(SUPER_ADMIN_ROLE));

                if (isSuperAdmin) {
                    TenantContextHolder.setTenantId(null);
                    log.debug("SUPER_ADMIN detected - tenant context set to null");
                } else {
                    Object principal = authentication.getPrincipal();
                    if (principal instanceof com.pharmacy.saas.security.service.CustomUserDetails userDetails) {
                        UUID tenantId = userDetails.getTenantId();
                        if (tenantId != null) {
                            TenantContextHolder.setTenantId(tenantId);
                            log.debug("Tenant context set for user {}: {}", userDetails.getUsername(), tenantId);
                        } else {
                            log.warn("User {} has no tenant_id", userDetails.getUsername());
                        }
                    }
                }
            }
        } catch (Exception e) {
            log.error("Error in TenantContextFilter: ", e);
            TenantContextHolder.clear();
        }

        try {
            filterChain.doFilter(request, response);
        } finally {
            TenantContextHolder.clear();
        }
    }

    @Override
    protected boolean shouldNotFilter(HttpServletRequest request) {
        String path = request.getRequestURI();
        return path.startsWith("/api/auth/login")
                || path.startsWith("/api/auth/forgot-password")
                || path.startsWith("/api/auth/reset-password")
                || path.startsWith("/api/auth/csrf")
                || path.startsWith("/actuator")
                || path.startsWith("/v3/api-docs")
                || path.startsWith("/swagger-ui");
    }
}