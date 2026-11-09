package com.pharmacy.saas.unit.service;

import com.pharmacy.saas.common.exception.BusinessException;
import com.pharmacy.saas.common.exception.DuplicateResourceException;
import com.pharmacy.saas.common.exception.ResourceNotFoundException;
import com.pharmacy.saas.common.util.TenantContextHolder;
import com.pharmacy.saas.role.entity.Role;
import com.pharmacy.saas.role.repository.RoleRepository;
import com.pharmacy.saas.user.dto.UserRequest;
import com.pharmacy.saas.user.dto.UserSearchCriteria;
import com.pharmacy.saas.user.dto.AssignRolesRequest;
import com.pharmacy.saas.user.dto.ChangePasswordRequest;
import com.pharmacy.saas.user.entity.User;
import com.pharmacy.saas.user.entity.UserStatus;
import com.pharmacy.saas.user.repository.UserRepository;
import com.pharmacy.saas.user.service.UserService;
import com.pharmacy.saas.user.service.UserValidationService;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageImpl;
import org.springframework.data.domain.PageRequest;
import org.springframework.security.crypto.password.PasswordEncoder;

import java.time.Instant;
import java.util.Collections;
import java.util.Optional;
import java.util.Set;
import java.util.UUID;

import static org.assertj.core.api.Assertions.*;
import static org.mockito.ArgumentMatchers.*;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
class UserServiceTest {

    @Mock
    private UserRepository userRepository;

    @Mock
    private RoleRepository roleRepository;

    @Mock
    private UserValidationService validationService;

    @Mock
    private PasswordEncoder passwordEncoder;

    @InjectMocks
    private UserService userService;

    private UUID tenantId;
    private UUID userId;

    @BeforeEach
    void setUp() {
        tenantId = UUID.randomUUID();
        userId = UUID.randomUUID();
        TenantContextHolder.setTenantId(tenantId);
    }

    @Test
    void createUser_shouldCreateUserSuccessfully() {
        UserRequest request = UserRequest.builder()
                .firstName("John")
                .lastName("Doe")
                .email("john.doe@example.com")
                .password("Test@123")
                .build();

        User savedUser = User.builder()
                .id(userId)
                .tenantId(tenantId)
                .firstName("John")
                .lastName("Doe")
                .email("john.doe@example.com")
                .passwordHash("hashed")
                .status(UserStatus.ACTIVE)
                .pwdChangedAt(Instant.now())
                .build();

        when(passwordEncoder.encode("Test@123")).thenReturn("hashed");
        when(userRepository.save(any(User.class))).thenReturn(savedUser);

        var response = userService.createUser(request);

        assertThat(response.getId()).isEqualTo(userId);
        assertThat(response.getEmail()).isEqualTo("john.doe@example.com");
        assertThat(response.getFirstName()).isEqualTo("John");
        verify(userRepository).save(any(User.class));
    }

    @Test
    void createUser_shouldThrowWhenEmailExists() {
        UserRequest request = UserRequest.builder()
                .firstName("John")
                .lastName("Doe")
                .email("john.doe@example.com")
                .password("Test@123")
                .build();

        doThrow(new DuplicateResourceException("User", "email", "john.doe@example.com"))
                .when(validationService).validateEmailUnique(anyString(), any(), any());

        assertThatThrownBy(() -> userService.createUser(request))
                .isInstanceOf(DuplicateResourceException.class)
                .hasMessageContaining("email");
    }

    @Test
    void getUser_shouldReturnUser() {
        User user = User.builder()
                .id(userId)
                .tenantId(tenantId)
                .firstName("John")
                .lastName("Doe")
                .email("john.doe@example.com")
                .status(UserStatus.ACTIVE)
                .build();

        when(userRepository.findByIdAndTenantIdWithRoles(userId, tenantId)).thenReturn(Optional.of(user));

        var response = userService.getUser(userId);

        assertThat(response.getId()).isEqualTo(userId);
        assertThat(response.getEmail()).isEqualTo("john.doe@example.com");
    }

    @Test
    void getUser_shouldThrowWhenNotFound() {
        when(userRepository.findByIdAndTenantId(userId, tenantId)).thenReturn(Optional.empty());

        assertThatThrownBy(() -> userService.getUser(userId))
                .isInstanceOf(ResourceNotFoundException.class);
    }

    @Test
    void changePassword_shouldUpdatePassword() {
        User user = User.builder()
                .id(userId)
                .tenantId(tenantId)
                .email("john.doe@example.com")
                .passwordHash("old_hashed")
                .build();

        ChangePasswordRequest request = ChangePasswordRequest.builder()
                .currentPassword("OldPass@123")
                .newPassword("NewPass@123")
                .confirmPassword("NewPass@123")
                .build();

        when(userRepository.findById(userId)).thenReturn(Optional.of(user));
        when(passwordEncoder.matches("OldPass@123", "old_hashed")).thenReturn(true);
        when(passwordEncoder.matches("NewPass@123", "old_hashed")).thenReturn(false);
        when(passwordEncoder.encode("NewPass@123")).thenReturn("new_hashed");

        userService.changePassword(userId, request);

        verify(userRepository).save(argThat(u ->
                u.getPasswordHash().equals("new_hashed") &&
                u.getPwdChangedAt() != null
        ));
    }

    @Test
    void changePassword_shouldThrowWhenCurrentPasswordWrong() {
        User user = User.builder()
                .id(userId)
                .passwordHash("old_hashed")
                .build();

        ChangePasswordRequest request = ChangePasswordRequest.builder()
                .currentPassword("WrongPass@123")
                .newPassword("NewPass@123")
                .confirmPassword("NewPass@123")
                .build();

        when(userRepository.findById(userId)).thenReturn(Optional.of(user));
        when(passwordEncoder.matches("WrongPass@123", "old_hashed")).thenReturn(false);

        assertThatThrownBy(() -> userService.changePassword(userId, request))
                .isInstanceOf(BusinessException.class)
                .hasMessageContaining("Current password is incorrect");
    }

    @Test
    void searchUsers_shouldReturnPagedResults() {
        User user = User.builder()
                .id(userId)
                .tenantId(tenantId)
                .firstName("John")
                .lastName("Doe")
                .email("john.doe@example.com")
                .status(UserStatus.ACTIVE)
                .build();

        Page<User> page = new PageImpl<>(Collections.singletonList(user), PageRequest.of(0, 20), 1);
        when(userRepository.search(eq(tenantId), anyString(), any(), any())).thenReturn(page);

        UserSearchCriteria criteria = UserSearchCriteria.builder()
                .search("john")
                .page(0)
                .size(20)
                .build();

        var response = userService.searchUsers(criteria);

        assertThat(response.getContent()).hasSize(1);
        assertThat(response.getTotalElements()).isEqualTo(1);
    }

    @Test
    void assignRoles_shouldUpdateUserRoles() {
        User user = User.builder()
                .id(userId)
                .tenantId(tenantId)
                .build();

        Role role = Role.builder()
                .id(UUID.randomUUID())
                .name("PHARMACIST")
                .build();

        AssignRolesRequest request = AssignRolesRequest.builder()
                .roleIds(Set.of(role.getId()))
                .build();

        when(userRepository.findByIdAndTenantIdWithRoles(userId, tenantId)).thenReturn(Optional.of(user));
        when(roleRepository.findAllById(Set.of(role.getId()))).thenReturn(Collections.singletonList(role));
        when(userRepository.save(any(User.class))).thenReturn(user);

        var response = userService.assignRoles(userId, request);

        assertThat(response.getRoles()).hasSize(1);
        verify(userRepository).save(any(User.class));
    }
}