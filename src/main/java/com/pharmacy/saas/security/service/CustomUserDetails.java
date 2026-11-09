package com.pharmacy.saas.security.service;

import org.springframework.security.core.GrantedAuthority;
import org.springframework.security.core.authority.SimpleGrantedAuthority;
import org.springframework.security.core.userdetails.UserDetails;

import java.time.Instant;
import java.util.Collection;
import java.util.List;
import java.util.Set;
import java.util.UUID;
import java.util.stream.Collectors;

public class CustomUserDetails implements UserDetails {

    private final UUID userId;
    private final UUID tenantId;
    private final String email;
    private final String passwordHash;
    private final String firstName;
    private final String lastName;
    private final String status;
    private final Instant lastLoginAt;
    private final Set<String> permissions;
    private final List<GrantedAuthority> authorities;
    private Instant currentLastLoginAt;

    public CustomUserDetails(
            UUID userId,
            UUID tenantId,
            String email,
            String passwordHash,
            String firstName,
            String lastName,
            String status,
            Instant lastLoginAt,
            Set<String> permissions,
            List<String> roles
    ) {
        this.userId = userId;
        this.tenantId = tenantId;
        this.email = email;
        this.passwordHash = passwordHash;
        this.firstName = firstName;
        this.lastName = lastName;
        this.status = status;
        this.lastLoginAt = lastLoginAt;
        this.permissions = permissions;
        this.currentLastLoginAt = lastLoginAt;

        this.authorities = roles.stream()
                .map(role -> new SimpleGrantedAuthority("ROLE_" + role))
                .collect(Collectors.toList());

        this.authorities.addAll(permissions.stream()
                .map(perm -> new SimpleGrantedAuthority("PERM_" + perm))
                .collect(Collectors.toList()));
    }

    @Override
    public Collection<? extends GrantedAuthority> getAuthorities() {
        return authorities;
    }

    @Override
    public String getPassword() {
        return passwordHash;
    }

    @Override
    public String getUsername() {
        return email;
    }

    @Override
    public boolean isAccountNonExpired() {
        return true;
    }

    @Override
    public boolean isAccountNonLocked() {
        return !"LOCKED".equals(status);
    }

    @Override
    public boolean isCredentialsNonExpired() {
        return true;
    }

    @Override
    public boolean isEnabled() {
        return "ACTIVE".equals(status);
    }

    public UUID getUserId() {
        return userId;
    }

    public UUID getTenantId() {
        return tenantId;
    }

    public String getFirstName() {
        return firstName;
    }

    public String getLastName() {
        return lastName;
    }

    public String getStatus() {
        return status;
    }

    public Instant getLastLoginAt() {
        return currentLastLoginAt;
    }

    public Set<String> getPermissions() {
        return permissions;
    }

    public List<String> getRoles() {
        return authorities.stream()
                .filter(a -> a.getAuthority().startsWith("ROLE_"))
                .map(a -> a.getAuthority().substring(5))
                .collect(Collectors.toList());
    }

    public void updateLastLogin(Instant lastLogin) {
        this.currentLastLoginAt = lastLogin;
    }

    public boolean hasRole(String role) {
        return getRoles().contains(role);
    }

    public boolean hasPermission(String permission) {
        return permissions.contains(permission);
    }
}