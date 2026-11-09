package com.pharmacy.saas.security.session;

import com.pharmacy.saas.common.exception.BusinessException;
import com.pharmacy.saas.common.constants.ErrorCodes;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;

import java.util.Set;
import java.util.UUID;

@Slf4j
@Service
@RequiredArgsConstructor
public class SessionManagementService {

    private final SessionRegistry sessionRegistry;

    @Value("${app.security.max-concurrent-sessions:5}")
    private int maxConcurrentSessions;

    public void registerSession(UUID userId, String sessionId) {
        int currentCount = sessionRegistry.getSessionCount(userId);
        if (currentCount >= maxConcurrentSessions) {
            Set<String> sessions = sessionRegistry.getUserSessions(userId);
            if (!sessions.isEmpty()) {
                String oldestSession = sessions.iterator().next();
                sessionRegistry.unregisterSession(oldestSession);
                log.info("Invalidated oldest session {} for user {} due to concurrent session limit",
                        oldestSession, userId);
            }
        }
        sessionRegistry.registerSession(userId, sessionId);
    }

    public void unregisterSession(String sessionId) {
        sessionRegistry.unregisterSession(sessionId);
    }

    public void invalidateAllUserSessions(UUID userId) {
        sessionRegistry.clearUserSessions(userId);
        log.info("Invalidated all sessions for user {}", userId);
    }

    public void invalidateAllUserSessionsExcept(UUID userId, String exceptSessionId) {
        Set<String> sessions = sessionRegistry.getUserSessions(userId);
        sessions.stream()
                .filter(s -> !s.equals(exceptSessionId))
                .forEach(sessionRegistry::unregisterSession);
        log.info("Invalidated all sessions except {} for user {}", exceptSessionId, userId);
    }

    public void enforceMaxSessions(UUID userId) {
        int currentCount = sessionRegistry.getSessionCount(userId);
        if (currentCount > maxConcurrentSessions) {
            Set<String> sessions = sessionRegistry.getUserSessions(userId);
            int toRemove = currentCount - maxConcurrentSessions;
            sessions.stream()
                    .limit(toRemove)
                    .forEach(sessionRegistry::unregisterSession);
            log.info("Enforced max sessions for user {}, removed {}", userId, toRemove);
        }
    }

    public int getActiveSessionCount(UUID userId) {
        return sessionRegistry.getSessionCount(userId);
    }

    public boolean isSessionValid(String sessionId) {
        return sessionRegistry.getUserId(sessionId) != null;
    }

    public UUID getUserIdFromSession(String sessionId) {
        return sessionRegistry.getUserId(sessionId);
    }
}