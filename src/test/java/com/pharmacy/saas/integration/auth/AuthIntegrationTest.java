package com.pharmacy.saas.integration.auth;

import com.pharmacy.saas.support.TestContainersConfig;
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

import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.*;

@SpringBootTest
@AutoConfigureMockMvc
@ContextConfiguration(classes = TestContainersConfig.class)
@Transactional
class AuthIntegrationTest {

    @Autowired
    private MockMvc mockMvc;

    @Autowired
    private UserRepository userRepository;

    @Autowired
    private PasswordEncoder passwordEncoder;

    @Test
    void login_shouldReturnSessionCookie() throws Exception {
        // Given
        UUID tenantId = UUID.randomUUID();
        User user = User.builder()
                .tenantId(tenantId)
                .firstName("Test")
                .lastName("User")
                .email("test@example.com")
                .passwordHash(passwordEncoder.encode("Test@123"))
                .status(UserStatus.ACTIVE)
                .build();
        userRepository.save(user);

        // When/Then
        mockMvc.perform(post("/api/auth/login")
                        .contentType(MediaType.APPLICATION_FORM_URLENCODED)
                        .param("email", "test@example.com")
                        .param("password", "Test@123"))
                .andExpect(status().isOk())
                .andExpect(cookie().exists("SESSION"))
                .andExpect(jsonPath("$.success").value(true))
                .andExpect(jsonPath("$.data.email").value("test@example.com"));
    }

    @Test
    void login_shouldFailWithWrongPassword() throws Exception {
        UUID tenantId = UUID.randomUUID();
        User user = User.builder()
                .tenantId(tenantId)
                .firstName("Test")
                .lastName("User")
                .email("test@example.com")
                .passwordHash(passwordEncoder.encode("Test@123"))
                .status(UserStatus.ACTIVE)
                .build();
        userRepository.save(user);

        mockMvc.perform(post("/api/auth/login")
                        .contentType(MediaType.APPLICATION_FORM_URLENCODED)
                        .param("email", "test@example.com")
                        .param("password", "WrongPass@123"))
                .andExpect(status().isUnauthorized())
                .andExpect(jsonPath("$.success").value(false))
                .andExpect(jsonPath("$.errorCode").value("INVALID_CREDENTIALS"));
    }

    @Test
    void login_shouldFailForInactiveUser() throws Exception {
        UUID tenantId = UUID.randomUUID();
        User user = User.builder()
                .tenantId(tenantId)
                .firstName("Test")
                .lastName("User")
                .email("test@example.com")
                .passwordHash(passwordEncoder.encode("Test@123"))
                .status(UserStatus.INACTIVE)
                .build();
        userRepository.save(user);

        mockMvc.perform(post("/api/auth/login")
                        .contentType(MediaType.APPLICATION_FORM_URLENCODED)
                        .param("email", "test@example.com")
                        .param("password", "Test@123"))
                .andExpect(status().isForbidden())
                .andExpect(jsonPath("$.success").value(false))
                .andExpect(jsonPath("$.errorCode").value("ACCOUNT_DISABLED"));
    }

    @Test
    void forgotPassword_shouldReturnSuccessEvenForNonExistentEmail() throws Exception {
        mockMvc.perform(post("/api/auth/forgot-password")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{\"email\":\"nonexistent@example.com\"}"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.success").value(true));
    }
}