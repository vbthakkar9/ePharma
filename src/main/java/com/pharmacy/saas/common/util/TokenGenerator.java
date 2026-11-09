package com.pharmacy.saas.common.util;

import lombok.extern.slf4j.Slf4j;

import java.nio.charset.StandardCharsets;
import java.security.MessageDigest;
import java.security.NoSuchAlgorithmException;
import java.security.SecureRandom;
import java.util.Base64;
import java.util.UUID;

@Slf4j
public final class TokenGenerator {

    private static final SecureRandom secureRandom = new SecureRandom();
    private static final int TOKEN_BYTES = 32;

    private TokenGenerator() {}

    public static String generateSecureToken() {
        byte[] tokenBytes = new byte[TOKEN_BYTES];
        secureRandom.nextBytes(tokenBytes);
        return Base64.getUrlEncoder().withoutPadding().encodeToString(tokenBytes);
    }

    public static String generateTenantCode(String pharmacyName) {
        String base = pharmacyName.toUpperCase()
            .replaceAll("[^A-Z0-9]", "")
            .substring(0, Math.min(6, pharmacyName.length()));
        String suffix = String.format("%04d", secureRandom.nextInt(10000));
        return base + suffix;
    }

    public static String hashToken(String token) {
        try {
            MessageDigest digest = MessageDigest.getInstance("SHA-256");
            byte[] hash = digest.digest(token.getBytes(StandardCharsets.UTF_8));
            return Base64.getEncoder().encodeToString(hash);
        } catch (NoSuchAlgorithmException e) {
            log.error("SHA-256 algorithm not available", e);
            throw new IllegalStateException("SHA-256 algorithm not available", e);
        }
    }

    public static String generateResetToken() {
        return UUID.randomUUID().toString().replace("-", "") +
               UUID.randomUUID().toString().replace("-", "");
    }

    public static String generateApiKey() {
        return "phsa_" + generateSecureToken();
    }

    public static boolean constantTimeEquals(String a, String b) {
        if (a == null || b == null) {
            return false;
        }
        if (a.length() != b.length()) {
            return false;
        }
        int result = 0;
        for (int i = 0; i < a.length(); i++) {
            result |= a.charAt(i) ^ b.charAt(i);
        }
        return result == 0;
    }
}