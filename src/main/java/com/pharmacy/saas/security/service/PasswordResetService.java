package com.pharmacy.saas.security.service;

import com.pharmacy.saas.common.exception.BusinessException;
import com.pharmacy.saas.common.exception.ResourceNotFoundException;
import com.pharmacy.saas.common.util.PasswordUtils;
import com.pharmacy.saas.common.util.TokenGenerator;
import com.pharmacy.saas.security.entity.PasswordResetToken;
import com.pharmacy.saas.security.repository.PasswordResetTokenRepository;
import com.pharmacy.saas.user.entity.User;
import com.pharmacy.saas.user.entity.UserStatus;
import com.pharmacy.saas.user.repository.UserRepository;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.Instant;
import java.util.Optional;
import java.util.UUID;

@Slf4j
@Service
@RequiredArgsConstructor
public class PasswordResetService {

    private final UserRepository userRepository;
    private final PasswordResetTokenRepository tokenRepository;
    private final PasswordEncoder passwordEncoder;

    @Value("${app.security.password-reset-token-expiry-minutes:15}")
    private int tokenExpiryMinutes;

    @Transactional
    public void requestPasswordReset(String email) {
        UUID tenantId = com.pharmacy.saas.common.util.TenantContextHolder.getTenantId();

        User user;
        if (tenantId != null) {
            user = userRepository.findByEmailAndTenantId(email, tenantId)
                    .orElse(null);
        } else {
            user = userRepository.findByEmailWithoutTenantFilter(email)
                    .orElse(null);
        }

        if (user == null) {
            log.info("Password reset requested for non-existent email: {}", email);
            return;
        }

        if (user.getStatus() != UserStatus.ACTIVE) {
            log.info("Password reset requested for inactive user: {}", email);
            return;
        }

        tokenRepository.findByUserIdAndUsedFalse(user.getId())
                .forEach(token -> {
                    token.setUsed(true);
                    tokenRepository.save(token);
                });

        String rawToken = TokenGenerator.generateResetToken();
        String tokenHash = PasswordUtils.hashToken(rawToken);

        PasswordResetToken resetToken = PasswordResetToken.builder()
                .userId(user.getId())
                .tokenHash(tokenHash)
                .expiresAt(Instant.now().plus(java.time.Duration.ofMinutes(tokenExpiryMinutes)))
                .used(false)
                .build();

        tokenRepository.save(resetToken);

        log.info("Password reset token for {}: {}", email, rawToken);
        log.info("Token expires at: {}", resetToken.getExpiresAt());
    }

    @Transactional
    public void resetPassword(String token, String newPassword) {
        String tokenHash = PasswordUtils.hashToken(token);

        PasswordResetToken resetToken = tokenRepository.findByTokenHash(tokenHash)
                .orElseThrow(() -> new BusinessException("RESET_TOKEN_INVALID", "Invalid or expired reset token"));

        if (resetToken.isUsed()) {
            throw new BusinessException("RESET_TOKEN_ALREADY_USED", "This reset token has already been used");
        }

        if (resetToken.getExpiresAt().isBefore(Instant.now())) {
            throw new BusinessException("RESET_TOKEN_EXPIRED", "Reset token has expired");
        }

        PasswordUtils.ValidationResult validation = PasswordUtils.validatePassword(newPassword);
        if (!validation.isValid()) {
            throw new BusinessException("PASSWORD_TOO_WEAK", String.join(", ", validation.getErrors()));
        }

        User user = userRepository.findById(resetToken.getUserId())
                .orElseThrow(() -> new ResourceNotFoundException("User", resetToken.getUserId()));

        user.setPasswordHash(passwordEncoder.encode(newPassword));
        user.setPwdChangedAt(Instant.now());
        user.setFailedLoginAttempts(0);
        user.setLockedUntil(null);
        if (user.getStatus() == UserStatus.LOCKED) {
            user.setStatus(UserStatus.ACTIVE);
        }
        userRepository.save(user);

        resetToken.setUsed(true);
        tokenRepository.save(resetToken);

        log.info("Password reset completed for user: {}", user.getEmail());
    }

    @Transactional(readOnly = true)
    public boolean validateResetToken(String token) {
        String tokenHash = PasswordUtils.hashToken(token);
        return tokenRepository.findByTokenHash(tokenHash)
                .map(rt -> !rt.isUsed() && rt.getExpiresAt().isAfter(Instant.now()))
                .orElse(false);
    }
}