package com.pharmacy.saas.security.session;

import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Component;

import java.util.Map;
import java.util.Set;
import java.util.UUID;
import java.util.concurrent.ConcurrentHashMap;

@Slf4j
@Component
public class SessionRegistry {

    private final Map<UUID, Set<String>> userSessions = new ConcurrentHashMap<>();
    private final Map<String, UUID> sessionToUser = new ConcurrentHashMap<>();

    public void registerSession(UUID userId, String sessionId) {
        userSessions.computeIfAbsent(userId, k -> ConcurrentHashMap.newKeySet())
                .add(sessionId);
        sessionToUser.put(sessionId, userId);
        log.debug("Registered session {} for user {}", sessionId, userId);
    }

    public void unregisterSession(String sessionId) {
        UUID userId = sessionToUser.remove(sessionId);
        if (userId != null) {
            Set<String> sessions = userSessions.get(userId);
            if (sessions != null) {
                sessions.remove(sessionId);
                if (sessions.isEmpty()) {
                    userSessions.remove(userId);
                }
            }
            log.debug("Unregistered session {} for user {}", sessionId, userId);
        }
    }

    public Set<String> getUserSessions(UUID userId) {
        return userSessions.getOrDefault(userId, Set.of());
    }

    public int getSessionCount(UUID userId) {
        Set<String> sessions = userSessions.get(userId);
        return sessions != null ? sessions.size() : 0;
    }

    public UUID getUserId(String sessionId) {
        return sessionToUser.get(sessionId);
    }

    public void clearUserSessions(UUID userId) {
        Set<String> sessions = userSessions.remove(userId);
        if (sessions != null) {
            sessions.forEach(sessionToUser::remove);
            log.debug("Cleared all {} sessions for user {}", sessions.size(), userId);
        }
    }

    public void clearAll() {
        userSessions.clear();
        sessionToUser.clear();
    }
}