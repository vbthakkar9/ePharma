package com.pharmacy.saas.security.filter;

import com.pharmacy.saas.audit.service.AuditEventPublisher;
import com.pharmacy.saas.common.constants.AuditActions;
import com.pharmacy.saas.common.util.TenantContextHolder;
import jakarta.servlet.FilterChain;
import jakarta.servlet.ServletException;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.stereotype.Component;
import org.springframework.web.filter.OncePerRequestFilter;

import java.io.IOException;
import java.time.Instant;
import java.util.Enumeration;
import java.util.HashMap;
import java.util.Map;
import java.util.UUID;

@Slf4j
@Component
@RequiredArgsConstructor
public class AuditLoggingFilter extends OncePerRequestFilter {

    private final AuditEventPublisher auditEventPublisher;

    private static final String[] SENSITIVE_HEADERS = {"authorization", "cookie", "x-csrf-token"};
    private static final String[] SENSITIVE_PARAMS = {"password", "newPassword", "confirmPassword", "currentPassword", "token"};

    @Override
    protected void doFilterInternal(
            HttpServletRequest request,
            HttpServletResponse response,
            FilterChain filterChain
    ) throws ServletException, IOException {

        String method = request.getMethod();
        boolean shouldAudit = switch (method) {
            case "POST", "PUT", "PATCH", "DELETE" -> true;
            default -> false;
        };

        String path = request.getRequestURI();
        if (path.startsWith("/actuator")
                || path.startsWith("/v3/api-docs")
                || path.startsWith("/swagger-ui")
                || path.equals("/api/auth/csrf")) {
            shouldAudit = false;
        }

        String ipAddress = getClientIpAddress(request);
        String userAgent = request.getHeader("User-Agent");
        Map<String, Object> requestMetadata = new HashMap<>();

        if (shouldAudit) {
            Map<String, String[]> paramMap = request.getParameterMap();
            for (Map.Entry<String, String[]> entry : paramMap.entrySet()) {
                if (!isSensitiveParam(entry.getKey())) {
                    String[] values = entry.getValue();
                    requestMetadata.put(entry.getKey(), values.length == 1 ? values[0] : values);
                } else {
                    requestMetadata.put(entry.getKey(), "[REDACTED]");
                }
            }

            Enumeration<String> headerNames = request.getHeaderNames();
            while (headerNames.hasMoreElements()) {
                String headerName = headerNames.nextElement();
                if (!isSensitiveHeader(headerName)) {
                    requestMetadata.put("header." + headerName, request.getHeader(headerName));
                }
            }
        }

        long startTime = System.currentTimeMillis();

        try {
            filterChain.doFilter(request, response);
        } finally {
            if (shouldAudit) {
                long duration = System.currentTimeMillis() - startTime;
                int status = response.getStatus();

                String action = determineAction(path, method);

                Authentication auth = SecurityContextHolder.getContext().getAuthentication();
                UUID userId = null;
                UUID tenantId = TenantContextHolder.getTenantId();

                if (auth != null && auth.getPrincipal() instanceof com.pharmacy.saas.security.service.CustomUserDetails userDetails) {
                    userId = userDetails.getUserId();
                }

                Map<String, Object> metadata = new HashMap<>(requestMetadata);
                metadata.put("method", method);
                metadata.put("path", path);
                metadata.put("status", status);
                metadata.put("duration_ms", duration);
                metadata.put("ip_address", ipAddress);
                metadata.put("user_agent", userAgent);

                auditEventPublisher.publish(
                        action,
                        extractEntityType(path),
                        extractEntityId(path),
                        metadata,
                        tenantId,
                        userId,
                        ipAddress,
                        userAgent
                );
            }
        }
    }

    private String getClientIpAddress(HttpServletRequest request) {
        String xForwardedFor = request.getHeader("X-Forwarded-For");
        if (xForwardedFor != null && !xForwardedFor.isEmpty()) {
            return xForwardedFor.split(",")[0].trim();
        }
        String xRealIp = request.getHeader("X-Real-IP");
        if (xRealIp != null && !xRealIp.isEmpty()) {
            return xRealIp;
        }
        return request.getRemoteAddr();
    }

    private boolean isSensitiveHeader(String headerName) {
        String lower = headerName.toLowerCase();
        for (String sensitive : SENSITIVE_HEADERS) {
            if (lower.equals(sensitive)) return true;
        }
        return false;
    }

    private boolean isSensitiveParam(String paramName) {
        String lower = paramName.toLowerCase();
        for (String sensitive : SENSITIVE_PARAMS) {
            if (lower.contains(sensitive)) return true;
        }
        return false;
    }

    private String determineAction(String path, String method) {
        if (path.startsWith("/api/auth/")) {
            if (path.contains("login")) return method.equals("POST") ? AuditActions.LOGIN : AuditActions.LOGIN_FAILED;
            if (path.contains("logout")) return AuditActions.LOGOUT;
            if (path.contains("change-password")) return AuditActions.PASSWORD_CHANGED;
            if (path.contains("forgot-password")) return AuditActions.PASSWORD_RESET_REQUESTED;
            if (path.contains("reset-password")) return AuditActions.PASSWORD_RESET_COMPLETED;
        }

        if (path.startsWith("/api/tenants/")) {
            if (method.equals("POST")) return AuditActions.TENANT_CREATED;
            if (method.equals("PUT") || method.equals("PATCH")) {
                if (path.contains("/status")) return AuditActions.TENANT_STATUS_CHANGED;
                return AuditActions.TENANT_UPDATED;
            }
            if (method.equals("DELETE")) return AuditActions.TENANT_DELETED;
        }

        if (path.startsWith("/api/users/")) {
            if (method.equals("POST")) return AuditActions.USER_CREATED;
            if (method.equals("PUT") || method.equals("PATCH")) {
                if (path.contains("/status")) return AuditActions.USER_STATUS_CHANGED;
                if (path.contains("/roles")) return AuditActions.USER_ROLES_CHANGED;
                return AuditActions.USER_UPDATED;
            }
            if (method.equals("DELETE")) return AuditActions.USER_DELETED;
        }

        if (path.startsWith("/api/roles/")) {
            if (method.equals("POST")) return AuditActions.ROLE_CREATED;
            if (method.equals("PUT") || method.equals("PATCH")) return AuditActions.ROLE_UPDATED;
            if (method.equals("DELETE")) return AuditActions.ROLE_DELETED;
        }

        if (path.startsWith("/api/permissions/")) {
            if (method.equals("POST")) return AuditActions.PERMISSION_CREATED;
            if (method.equals("PUT") || method.equals("PATCH")) return AuditActions.PERMISSION_UPDATED;
            if (method.equals("DELETE")) return AuditActions.PERMISSION_DELETED;
        }

        return method + "_" + path.replace("/", "_").toUpperCase();
    }

    private String extractEntityType(String path) {
        if (path.startsWith("/api/tenants")) return "tenant";
        if (path.startsWith("/api/users")) return "user";
        if (path.startsWith("/api/roles")) return "role";
        if (path.startsWith("/api/permissions")) return "permission";
        if (path.startsWith("/api/auth")) return "auth";
        return "unknown";
    }

    private UUID extractEntityId(String path) {
        String[] parts = path.split("/");
        for (int i = 0; i < parts.length - 1; i++) {
            if (("tenants".equals(parts[i]) || "users".equals(parts[i])
                    || "roles".equals(parts[i]) || "permissions".equals(parts[i]))
                    && i + 1 < parts.length) {
                try {
                    return UUID.fromString(parts[i + 1]);
                } catch (IllegalArgumentException e) {
                }
            }
        }
        return null;
    }
}