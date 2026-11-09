package com.pharmacy.saas.security.service;

import com.pharmacy.saas.common.util.TenantContextHolder;
import com.pharmacy.saas.user.entity.User;
import com.pharmacy.saas.user.entity.UserStatus;
import com.pharmacy.saas.user.repository.UserRepository;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.security.core.userdetails.UserDetails;
import org.springframework.security.core.userdetails.UserDetailsService;
import org.springframework.security.core.userdetails.UsernameNotFoundException;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.Set;
import java.util.UUID;
import java.util.stream.Collectors;

@Slf4j
@Service
@RequiredArgsConstructor
public class CustomUserDetailsService implements UserDetailsService {

    private final UserRepository userRepository;

    @Override
    @Transactional(readOnly = true)
    public UserDetails loadUserByUsername(String email) throws UsernameNotFoundException {
        UUID tenantId = TenantContextHolder.getTenantId();

        User user;
        if (tenantId != null) {
            user = userRepository.findByEmailAndTenantId(email, tenantId)
                    .orElseThrow(() -> new UsernameNotFoundException("User not found: " + email));
        } else {
            user = userRepository.findByEmailWithoutTenantFilter(email)
                    .orElseThrow(() -> new UsernameNotFoundException("User not found: " + email));
        }

        if (user.getStatus() != UserStatus.ACTIVE) {
            if (user.getStatus() == UserStatus.LOCKED) {
                throw new org.springframework.security.authentication.LockedException("Account is locked");
            } else if (user.getStatus() == UserStatus.INACTIVE) {
                throw new org.springframework.security.authentication.DisabledException("Account is disabled");
            } else {
                throw new org.springframework.security.authentication.DisabledException("Account is not active");
            }
        }

        Set<String> roles = user.getUserRoles().stream()
                .map(ur -> ur.getRole().getName())
                .collect(Collectors.toSet());

        Set<String> permissions = user.getUserRoles().stream()
                .flatMap(ur -> ur.getRole().getRolePermissions().stream())
                .map(rp -> rp.getPermission().getName())
                .collect(Collectors.toSet());

        return new CustomUserDetails(
                user.getId(),
                user.getTenantId(),
                user.getEmail(),
                user.getPasswordHash(),
                user.getFirstName(),
                user.getLastName(),
                user.getStatus().name(),
                user.getLastLoginAt(),
                permissions,
                roles.stream().toList()
        );
    }

    @Transactional
    public void incrementFailedAttempts(String email) {
        UUID tenantId = TenantContextHolder.getTenantId();

        User user;
        if (tenantId != null) {
            user = userRepository.findByEmailAndTenantId(email, tenantId)
                    .orElse(null);
        } else {
            user = userRepository.findByEmailWithoutTenantFilter(email)
                    .orElse(null);
        }

        if (user != null) {
            int attempts = user.getFailedLoginAttempts() + 1;
            user.setFailedLoginAttempts(attempts);

            int maxAttempts = 5;
            if (attempts >= maxAttempts) {
                user.setStatus(UserStatus.LOCKED);
                user.setLockedUntil(java.time.Instant.now().plus(java.time.Duration.ofMinutes(30)));
                log.warn("User {} locked after {} failed attempts", email, attempts);
            }

            userRepository.save(user);
        }
    }

    @Transactional
    public void resetFailedAttempts(User user) {
        user.setFailedLoginAttempts(0);
        user.setLockedUntil(null);
        if (user.getStatus() == UserStatus.LOCKED) {
            user.setStatus(UserStatus.ACTIVE);
        }
        userRepository.save(user);
    }
}