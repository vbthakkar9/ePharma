package com.pharmacy.saas.user.controller;

import com.pharmacy.saas.common.response.ApiResponse;
import com.pharmacy.saas.common.response.PageResponse;
import com.pharmacy.saas.security.service.AuthenticationService;
import com.pharmacy.saas.user.dto.*;
import com.pharmacy.saas.user.entity.UserStatus;
import com.pharmacy.saas.user.service.UserService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.security.SecurityRequirement;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.security.core.Authentication;
import org.springframework.web.bind.annotation.*;

import java.util.UUID;

@Tag(name = "User Management", description = "Manage users within a tenant")
@SecurityRequirement(name = "sessionAuth")
@RestController
@RequestMapping("/users")
@RequiredArgsConstructor
public class UserController {

    private final UserService userService;
    private final AuthenticationService authenticationService;

    @Operation(summary = "Create a new user")
    @PostMapping
    @PreAuthorize("hasAnyRole('SUPER_ADMIN', 'PHARMACY_ADMIN')")
    public ResponseEntity<ApiResponse<UserResponse>> createUser(@Valid @RequestBody UserRequest request) {
        UserResponse response = userService.createUser(request);
        return ResponseEntity.ok(ApiResponse.success(response, "User created successfully"));
    }

    @Operation(summary = "Get current user profile")
    @GetMapping("/me")
    public ResponseEntity<ApiResponse<UserResponse>> getCurrentUserProfile() {
        // Get current user from authentication
        var profile = authenticationService.getCurrentUserProfile();
        UUID userId = UUID.fromString(profile.get("id").toString());
        UserResponse response = userService.getUser(userId);
        return ResponseEntity.ok(ApiResponse.success(response));
    }

    @Operation(summary = "Get user by ID")
    @GetMapping("/{id}")
    @PreAuthorize("hasAnyRole('SUPER_ADMIN', 'PHARMACY_ADMIN') or @userSecurityService.isSelf(#id)")
    public ResponseEntity<ApiResponse<UserResponse>> getUser(@PathVariable UUID id) {
        UserResponse response = userService.getUser(id);
        return ResponseEntity.ok(ApiResponse.success(response));
    }

    @Operation(summary = "Search users with pagination")
    @GetMapping
    @PreAuthorize("hasAnyRole('SUPER_ADMIN', 'PHARMACY_ADMIN')")
    public ResponseEntity<ApiResponse<PageResponse<UserResponse>>> searchUsers(
            @RequestParam(required = false) String search,
            @RequestParam(required = false) UserStatus status,
            @RequestParam(required = false) UUID roleId,
            @RequestParam(defaultValue = "0") int page,
            @RequestParam(defaultValue = "20") int size,
            @RequestParam(defaultValue = "createdAt,DESC") String sort
    ) {
        UserSearchCriteria criteria = UserSearchCriteria.builder()
                .search(search)
                .status(status)
                .roleId(roleId)
                .page(page)
                .size(size)
                .sort(sort)
                .build();

        PageResponse<UserResponse> response = userService.searchUsers(criteria);
        return ResponseEntity.ok(ApiResponse.success(response));
    }

    @Operation(summary = "Update user")
    @PutMapping("/{id}")
    @PreAuthorize("hasAnyRole('SUPER_ADMIN', 'PHARMACY_ADMIN')")
    public ResponseEntity<ApiResponse<UserResponse>> updateUser(
            @PathVariable UUID id,
            @Valid @RequestBody UserRequest request
    ) {
        UserResponse response = userService.updateUser(id, request);
        return ResponseEntity.ok(ApiResponse.success(response, "User updated successfully"));
    }

    @Operation(summary = "Change user status")
    @PatchMapping("/{id}/status")
    @PreAuthorize("hasAnyRole('SUPER_ADMIN', 'PHARMACY_ADMIN')")
    public ResponseEntity<ApiResponse<UserResponse>> changeStatus(
            @PathVariable UUID id,
            @RequestParam UserStatus status
    ) {
        UserResponse response = userService.changeStatus(id, status);
        return ResponseEntity.ok(ApiResponse.success(response, "User status changed to " + status));
    }

    @Operation(summary = "Assign roles to user")
    @PatchMapping("/{id}/roles")
    @PreAuthorize("hasAnyRole('SUPER_ADMIN', 'PHARMACY_ADMIN')")
    public ResponseEntity<ApiResponse<UserResponse>> assignRoles(
            @PathVariable UUID id,
            @Valid @RequestBody AssignRolesRequest request
    ) {
        UserResponse response = userService.assignRoles(id, request);
        return ResponseEntity.ok(ApiResponse.success(response, "Roles assigned successfully"));
    }

    @Operation(summary = "Change password")
    @PostMapping("/change-password")
    public ResponseEntity<ApiResponse<Void>> changePassword(@Valid @RequestBody ChangePasswordRequest request) {
        UUID userId = authenticationService.getCurrentUserId();
        userService.changePassword(userId, request);
        return ResponseEntity.ok(ApiResponse.success(null, "Password changed successfully"));
    }

    @Operation(summary = "Delete user")
    @DeleteMapping("/{id}")
    @PreAuthorize("hasAnyRole('SUPER_ADMIN', 'PHARMACY_ADMIN') and not @userSecurityService.isSelf(#id)")
    public ResponseEntity<ApiResponse<Void>> deleteUser(@PathVariable UUID id) {
        userService.deleteUser(id);
        return ResponseEntity.ok(ApiResponse.success(null, "User deleted successfully"));
    }
}