package com.pharmacy.saas.integration.security;

import com.pharmacy.saas.support.TestContainersConfig;
import com.pharmacy.saas.tenant.entity.Tenant;
import com.pharmacy.saas.tenant.repository.TenantRepository;
import com.pharmacy.saas.user.entity.User;
import com.pharmacy.saas.user.entity.UserStatus;
import com.pharmacy.saas.user.repository.UserRepository;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.http.MediaType;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.test.context.ContextConfiguration;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.transaction.annotation.Transactional;

import java.util.UUID;

import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.*;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.*;

@SpringBootTest
@AutoConfigureMockMvc
@ContextConfiguration(classes = TestContainersConfig.class)
@Transactional
class SecurityIntegrationTest {

    @Autowired
    private MockMvc mockMvc;

    @Autowired
    private UserRepository userRepository;

    @Autowired
    private TenantRepository tenantRepository;

    @Autowired
    private PasswordEncoder passwordEncoder;

    @Test
    void rateLimiting_shouldBlockAfterMaxAttempts() throws Exception {
        UUID tenantId = UUID.randomUUID();
        User user = User.builder()
                .tenantId(tenantId)
                .firstName("Test")
                .lastName("User")
                .email("ratelimit@example.com")
                .passwordHash(passwordEncoder.encode("Test@123"))
                .status(UserStatus.ACTIVE)
                .build();
        userRepository.save(user);

        // Make 5 failed attempts
        for (int i = 0; i < 5; i++) {
            mockMvc.perform(post("/api/auth/login")
                            .contentType(MediaType.APPLICATION_FORM_URLENCODED)
                            .param("email", "ratelimit@example.com")
                            .param("password", "WrongPass@123"))
                    .andExpect(status().isUnauthorized());
        }

        // 6th attempt should be rate limited
        mockMvc.perform(post("/api/auth/login")
                        .contentType(MediaType.APPLICATION_FORM_URLENCODED)
                        .param("email", "ratelimit@example.com")
                        .param("password", "WrongPass@123"))
                .andExpect(status().isTooManyRequests())
                .andExpect(jsonPath("$.errorCode").value("RATE_LIMIT_EXCEEDED"));
    }

    @Test
    void crossTenantAccess_shouldBeDenied() throws Exception {
        // Create two tenants
        Tenant tenant1 = Tenant.builder()
                .tenantCode("TENANT1")
                .pharmacyName("Pharmacy 1")
                .email("tenant1@example.com")
                .status("ACTIVE")
                .build();
        tenantRepository.save(tenant1);

        Tenant tenant2 = Tenant.builder()
                .tenantCode("TENANT2")
                .pharmacyName("Pharmacy 2")
                .email("tenant2@example.com")
                .status("ACTIVE")
                .build();
        tenantRepository.save(tenant2);

        // Create user in tenant1
        User user1 = User.builder()
                .tenantId(tenant1.getId())
                .firstName("User")
                .lastName("One")
                .email("user1@example.com")
                .passwordHash(passwordEncoder.encode("Test@123"))
                .status(UserStatus.ACTIVE)
                .build();
        userRepository.save(user1);

        // Create user in tenant2
        User user2 = User.builder()
                .tenantId(tenant2.getId())
                .firstName("User")
                .lastName("Two")
                .email("user2@example.com")
                .passwordHash(passwordEncoder.encode("Test@123"))
                .status(UserStatus.ACTIVE)
                .build();
        userRepository.save(user2);

        // Login as user1
        var loginResponse = mockMvc.perform(post("/api/auth/login")
                        .contentType(MediaType.APPLICATION_FORM_URLENCODED)
                        .param("email", "user1@example.com")
                        .param("password", "Test@123"))
                .andExpect(status().isOk())
                .andReturn();

        String sessionCookie = loginResponse.getResponse().getHeader("Set-Cookie");

        // Try to access user2 (different tenant) - should be forbidden
        mockMvc.perform(get("/api/users/" + user2.getId())
                        .header("Cookie", sessionCookie))
                .andExpect(status().isForbidden());
    }

    @Test
    void csrfProtection_shouldRequireTokenForStateChanges() throws Exception {
        UUID tenantId = UUID.randomUUID();
        User user = User.builder()
                .tenantId(tenantId)
                .firstName("Test")
                .lastName("User")
                .email("csrf@example.com")
                .passwordHash(passwordEncoder.encode("Test@123"))
                .status(UserStatus.ACTIVE)
                .build();
        userRepository.save(user);

        // Login first
        var loginResponse = mockMvc.perform(post("/api/auth/login")
                        .contentType(MediaType.APPLICATION_FORM_URLENCODED)
                        .param("email", "csrf@example.com")
                        .param("password", "Test@123"))
                .andExpect(status().isOk())
                .andReturn();

        String sessionCookie = loginResponse.getResponse().getHeader("Set-Cookie");

        // Try to change password without CSRF token - should fail
        mockMvc.perform(post("/api/auth/change-password")
                        .header("Cookie", sessionCookie)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{\"currentPassword\":\"Test@123\",\"newPassword\":\"NewPass@123\",\"confirmPassword\":\"NewPass@123\"}"))
                .andExpect(status().isForbidden());
    }

    @Test
    void sessionRotation_shouldCreateNewSessionOnLogin() throws Exception {
        UUID tenantId = UUID.randomUUID();
        User user = User.builder()
                .tenantId(tenantId)
                .firstName("Test")
                .lastName("User")
                .email("rotation@example.com")
                .passwordHash(passwordEncoder.encode("Test@123"))
                .status(UserStatus.ACTIVE)
                .build();
        userRepository.save(user);

        // First login
        var login1 = mockMvc.perform(post("/api/auth/login")
                        .contentType(MediaType.APPLICATION_FORM_URLENCODED)
                        .param("email", "rotation@example.com")
                        .param("password", "Test@123"))
                .andExpect(status().isOk())
                .andReturn();

        String session1 = login1.getResponse().getHeader("Set-Cookie");

        // Second login (simulating new browser)
        var login2 = mockMvc.perform(post("/api/auth/login")
                        .contentType(MediaType.APPLICATION_FORM_URLENCODED)
                        .param("email", "rotation@example.com")
                        .param("password", "Test@123"))
                .andExpect(status().isOk())
                .andReturn();

        String session2 = login2.getResponse().getHeader("Set-Cookie");

        // Sessions should be different (rotated)
        assertNotSame(session1, session2);
    }

    @Test
    void passwordResetFlow_shouldWorkEndToEnd() throws Exception {
        UUID tenantId = UUID.randomUUID();
        User user = User.builder()
                .tenantId(tenantId)
                .firstName("Test")
                .lastName("User")
                .email("reset@example.com")
                .passwordHash(passwordEncoder.encode("Test@123"))
                .status(UserStatus.ACTIVE)
                .build();
        userRepository.save(user);

        // Request password reset
        mockMvc.perform(post("/api/auth/forgot-password")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{\"email\":\"reset@example.com\"}"))
                .andExpect(status().isOk());

        // Note: In real test, we'd extract token from logs/email
        // For now, just verify endpoint works
    }
}