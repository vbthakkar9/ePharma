package com.pharmacy.saas.user.service;

import com.pharmacy.saas.common.exception.BusinessException;
import com.pharmacy.saas.common.exception.ResourceNotFoundException;
import com.pharmacy.saas.common.response.PageResponse;
import com.pharmacy.saas.common.util.PasswordUtils;
import com.pharmacy.saas.common.util.TenantContextHolder;
import com.pharmacy.saas.role.entity.Role;
import com.pharmacy.saas.role.repository.RoleRepository;
import com.pharmacy.saas.user.dto.*;
import com.pharmacy.saas.user.entity.User;
import com.pharmacy.saas.user.entity.UserRole;
import com.pharmacy.saas.user.entity.UserStatus;
import com.pharmacy.saas.user.repository.UserRepository;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Pageable;
import org.springframework.data.domain.Sort;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.Instant;
import java.util.Set;
import java.util.UUID;
import java.util.stream.Collectors;

@Slf4j
@Service
@RequiredArgsConstructor
public class UserService {

    private final UserRepository userRepository;
    private final RoleRepository roleRepository;
    private final UserValidationService validationService;
    private final PasswordEncoder passwordEncoder;

    @Transactional
    public UserResponse createUser(UserRequest request) {
        UUID tenantId = TenantContextHolder.getRequiredTenantId();

        validationService.validateEmailUnique(request.getEmail(), tenantId, null);

        User user = User.builder()
                .tenantId(tenantId)
                .firstName(request.getFirstName())
                .lastName(request.getLastName())
                .email(request.getEmail().toLowerCase())
                .mobile(request.getMobile())
                .passwordHash(passwordEncoder.encode(request.getPassword()))
                .status(UserStatus.ACTIVE)
                .pwdChangedAt(Instant.now())
                .build();

        // Assign roles if provided
        if (request.getRoleIds() != null && !request.getRoleIds().isEmpty()) {
            validationService.validateRolesExist(request.getRoleIds(), tenantId);
            Set<Role> roles = roleRepository.findAllById(request.getRoleIds())
                    .stream()
                    .collect(Collectors.toSet());
            assignRolesToUser(user, roles, null);
        }

        User saved = userRepository.save(user);
        log.info("Created user: {} in tenant {}", saved.getEmail(), tenantId);
        return toResponse(saved);
    }

    @Transactional(readOnly = true)
    public UserResponse getUser(UUID id) {
        User user = validationService.getUserOrThrow(id);
        return toResponse(user);
    }

    @Transactional(readOnly = true)
    public UserResponse getCurrentUserProfile() {
        UUID tenantId = TenantContextHolder.getTenantId();
        // Get current user ID from auth context
        // This will be handled by the controller using AuthenticationService
        throw new UnsupportedOperationException("Use AuthenticationService for current user profile");
    }

    @Transactional(readOnly = true)
    public PageResponse<UserResponse> searchUsers(UserSearchCriteria criteria) {
        UUID tenantId = TenantContextHolder.getRequiredTenantId();
        Pageable pageable = buildPageable(criteria);

        Page<User> users = userRepository.search(tenantId, criteria.getSearch(), criteria.getStatus(), criteria.getRoleId(), pageable);
        return PageResponse.from(users.map(this::toResponse));
    }

    @Transactional
    public UserResponse updateUser(UUID id, UserRequest request) {
        User user = validationService.getUserOrThrow(id);

        validationService.validateEmailUnique(request.getEmail(), user.getTenantId(), id);

        user.setFirstName(request.getFirstName());
        user.setLastName(request.getLastName());
        user.setEmail(request.getEmail().toLowerCase());
        user.setMobile(request.getMobile());

        User saved = userRepository.save(user);
        log.info("Updated user: {}", saved.getId());
        return toResponse(saved);
    }

    @Transactional
    public UserResponse changeStatus(UUID id, UserStatus newStatus) {
        User user = validationService.getUserOrThrow(id);
        validationService.validateStatusTransition(user.getStatus(), newStatus);

        UserStatus oldStatus = user.getStatus();
        user.setStatus(newStatus);

        // If unlocking, reset failed attempts
        if (newStatus == UserStatus.ACTIVE && oldStatus == UserStatus.LOCKED) {
            user.setFailedLoginAttempts(0);
            user.setLockedUntil(null);
        }

        User saved = userRepository.save(user);
        log.info("Changed user {} status from {} to {}", id, oldStatus, newStatus);
        return toResponse(saved);
    }

    @Transactional
    public UserResponse assignRoles(UUID id, AssignRolesRequest request) {
        User user = validationService.getUserOrThrow(id);
        UUID tenantId = TenantContextHolder.getRequiredTenantId();

        validationService.validateRolesExist(request.getRoleIds(), tenantId);

        Set<Role> roles = roleRepository.findAllById(request.getRoleIds())
                .stream()
                .collect(Collectors.toSet());

        // Clear existing roles and assign new ones
        user.getUserRoles().clear();
        assignRolesToUser(user, roles, getCurrentUserId());

        User saved = userRepository.save(user);
        log.info("Assigned roles to user {}: {}", id, request.getRoleIds());
        return toResponse(saved);
    }

    @Transactional
    public void changePassword(UUID userId, ChangePasswordRequest request) {
        User user = validationService.getUserOrThrow(userId);

        if (!passwordEncoder.matches(request.getCurrentPassword(), user.getPasswordHash())) {
            throw new BusinessException("INVALID_CURRENT_PASSWORD", "Current password is incorrect");
        }

        if (!request.getNewPassword().equals(request.getConfirmPassword())) {
            throw new BusinessException("PASSWORD_MISMATCH", "New password and confirmation do not match");
        }

        PasswordUtils.ValidationResult validation = PasswordUtils.validatePassword(request.getNewPassword());
        if (!validation.isValid()) {
            throw new BusinessException("PASSWORD_TOO_WEAK", String.join(", ", validation.getErrors()));
        }

        if (passwordEncoder.matches(request.getNewPassword(), user.getPasswordHash())) {
            throw new BusinessException("PASSWORD_REUSE_NOT_ALLOWED", "New password cannot be the same as current password");
        }

        user.setPasswordHash(passwordEncoder.encode(request.getNewPassword()));
        user.setPwdChangedAt(Instant.now());
        userRepository.save(user);

        log.info("Password changed for user: {}", userId);
    }

    @Transactional
    public void deleteUser(UUID id) {
        User user = validationService.getUserOrThrow(id);

        // Prevent self-deletion (handled in controller)

        userRepository.delete(user);
        log.info("Deleted user: {}", id);
    }

    private void assignRolesToUser(User user, Set<Role> roles, UUID assignedBy) {
        for (Role role : roles) {
            UserRole userRole = UserRole.builder()
                    .user(user)
                    .role(role)
                    .assignedAt(Instant.now())
                    .assignedBy(assignedBy)
                    .build();
            user.getUserRoles().add(userRole);
        }
    }

    private UUID getCurrentUserId() {
        // Would be extracted from authentication context
        return null;
    }

    private Pageable buildPageable(UserSearchCriteria criteria) {
        int page = Math.max(0, criteria.getPage());
        int size = Math.min(100, Math.max(1, criteria.getSize()));

        Sort sort = Sort.by(Sort.Direction.DESC, "createdAt");
        if (criteria.getSort() != null) {
            String[] parts = criteria.getSort().split(",");
            if (parts.length == 2) {
                Sort.Direction direction = "ASC".equalsIgnoreCase(parts[1]) ? Sort.Direction.ASC : Sort.Direction.DESC;
                sort = Sort.by(direction, parts[0]);
            }
        }

        return PageRequest.of(page, size, sort);
    }

    private UserResponse toResponse(User user) {
        Set<RoleResponse> roleResponses = user.getUserRoles().stream()
                .map(ur -> RoleResponse.builder()
                        .id(ur.getRole().getId())
                        .name(ur.getRole().getName())
                        .description(ur.getRole().getDescription())
                        .isSystem(ur.getRole().getIsSystem())
                        .build())
                .collect(Collectors.toSet());

        return UserResponse.builder()
                .id(user.getId())
                .tenantId(user.getTenantId())
                .firstName(user.getFirstName())
                .lastName(user.getLastName())
                .email(user.getEmail())
                .mobile(user.getMobile())
                .status(user.getStatus().name())
                .lastLoginAt(user.getLastLoginAt())
                .pwdChangedAt(user.getPwdChangedAt())
                .createdAt(user.getCreatedAt())
                .updatedAt(user.getUpdatedAt())
                .roles(roleResponses)
                .build();
    }
}