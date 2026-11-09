package com.pharmacy.saas.security.service;

import com.pharmacy.saas.common.exception.BusinessException;
import com.pharmacy.saas.common.exception.ResourceNotFoundException;
import com.pharmacy.saas.common.util.PasswordUtils;
import com.pharmacy.saas.common.util.TokenGenerator;
import com.pharmacy.saas.security.service.CustomUserDetails;
import com.pharmacy.saas.security.session.SessionManagementService;
import com.pharmacy.saas.user.entity.User;
import com.pharmacy.saas.user.entity.UserStatus;
import com.pharmacy.saas.user.repository.UserRepository;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.Instant;
import java.util.HashMap;
import java.util.Map;
import java.util.Optional;
import java.util.Set;
import java.util.UUID;
import java.util.stream.Collectors;

@Slf4j
@Service
@RequiredArgsConstructor
public class AuthenticationService {

    private final UserRepository userRepository;
    private final PasswordEncoder passwordEncoder;
    private final CustomUserDetailsService userDetailsService;
    private final PasswordResetService passwordResetService;
    private final SessionManagementService sessionManagementService;

    @Transactional(readOnly = true)
    public Map<String, Object> getCurrentUserProfile() {
        Authentication auth = SecurityContextHolder.getContext().getAuthentication();
        if (auth == null || !auth.isAuthenticated() || auth.getPrincipal().equals("anonymousUser")) {
            throw new BusinessException("UNAUTHENTICATED", "User not authenticated");
        }

        CustomUserDetails userDetails = (CustomUserDetails) auth.getPrincipal();

        Map<String, Object> profile = new HashMap<>();
        profile.put("id", userDetails.getUserId().toString());
        profile.put("email", userDetails.getUsername());
        profile.put("firstName", userDetails.getFirstName());
        profile.put("lastName", userDetails.getLastName());
        profile.put("tenantId", userDetails.getTenantId() != null ? userDetails.getTenantId().toString() : null);
        profile.put("roles", userDetails.getRoles());
        profile.put("permissions", userDetails.getPermissions());
        profile.put("lastLoginAt", userDetails.getLastLoginAt());

        return profile;
    }

    @Transactional
    public void changePassword(String currentPassword, String newPassword) {
        Authentication auth = SecurityContextHolder.getContext().getAuthentication();
        CustomUserDetails userDetails = (CustomUserDetails) auth.getPrincipal();

        User user = userRepository.findById(userDetails.getUserId())
                .orElseThrow(() -> new ResourceNotFoundException("User", userDetails.getUserId()));

        if (!passwordEncoder.matches(currentPassword, user.getPasswordHash())) {
            throw new BusinessException("INVALID_CURRENT_PASSWORD", "Current password is incorrect");
        }

        PasswordUtils.ValidationResult validation = PasswordUtils.validatePassword(newPassword);
        if (!validation.isValid()) {
            throw new BusinessException("PASSWORD_TOO_WEAK", String.join(", ", validation.getErrors()));
        }

        if (passwordEncoder.matches(newPassword, user.getPasswordHash())) {
            throw new BusinessException("PASSWORD_REUSE_NOT_ALLOWED", "New password cannot be the same as current password");
        }

        user.setPasswordHash(passwordEncoder.encode(newPassword));
        user.setPwdChangedAt(Instant.now());
        userRepository.save(user);

        sessionManagementService.invalidateAllUserSessionsExcept(user.getId(), null);

        log.info("Password changed for user: {}", user.getEmail());
    }

    @Transactional(readOnly = true)
    public boolean isAuthenticated() {
        Authentication auth = SecurityContextHolder.getContext().getAuthentication();
        return auth != null && auth.isAuthenticated() && !auth.getPrincipal().equals("anonymousUser");
    }

    @Transactional(readOnly = true)
    public Optional<CustomUserDetails> getCurrentUserDetails() {
        Authentication auth = SecurityContextHolder.getContext().getAuthentication();
        if (auth != null && auth.isAuthenticated() && auth.getPrincipal() instanceof CustomUserDetails ud) {
            return Optional.of(ud);
        }
        return Optional.empty();
    }

    @Transactional(readOnly = true)
    public UUID getCurrentUserId() {
        return getCurrentUserDetails()
                .map(CustomUserDetails::getUserId)
                .orElseThrow(() -> new BusinessException("UNAUTHENTICATED", "User not authenticated"));
    }

    @Transactional(readOnly = true)
    public UUID getCurrentTenantId() {
        return getCurrentUserDetails()
                .map(CustomUserDetails::getTenantId)
                .orElse(null);
    }

    @Transactional(readOnly = true)
    public boolean hasRole(String role) {
        return getCurrentUserDetails()
                .map(ud -> ud.hasRole(role))
                .orElse(false);
    }

    @Transactional(readOnly = true)
    public boolean hasPermission(String permission) {
        return getCurrentUserDetails()
                .map(ud -> ud.hasPermission(permission))
                .orElse(false);
    }

    @Transactional(readOnly = true)
    public boolean hasAnyRole(String... roles) {
        return getCurrentUserDetails()
                .map(ud -> {
                    for (String role : roles) {
                        if (ud.hasRole(role)) return true;
                    }
                    return false;
                })
                .orElse(false);
    }

    @Transactional(readOnly = true)
    public boolean hasAnyPermission(String... permissions) {
        return getCurrentUserDetails()
                .map(ud -> {
                    for (String perm : permissions) {
                        if (ud.hasPermission(perm)) return true;
                    }
                    return false;
                })
                .orElse(false);
    }
}