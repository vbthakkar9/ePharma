package com.pharmacy.saas.audit.service;

import com.pharmacy.saas.audit.entity.AuditLog;
import com.pharmacy.saas.audit.repository.AuditLogRepository;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.scheduling.annotation.Async;
import org.springframework.stereotype.Service;
import org.springframework.transaction.event.TransactionPhase;
import org.springframework.transaction.event.TransactionalEventListener;

import java.time.Instant;
import java.util.Map;
import java.util.UUID;

@Slf4j
@Service
@RequiredArgsConstructor
public class AuditEventPublisher {

    private final AuditLogRepository auditLogRepository;

    @Async("auditExecutor")
    @TransactionalEventListener(phase = TransactionPhase.AFTER_COMMIT)
    public void publish(AuditEvent event) {
        try {
            AuditLog auditLog = AuditLog.builder()
                    .tenantId(event.getTenantId())
                    .userId(event.getUserId())
                    .action(event.getAction())
                    .entityType(event.getEntityType())
                    .entityId(event.getEntityId())
                    .metadata(event.getMetadata())
                    .ipAddress(event.getIpAddress())
                    .userAgent(event.getUserAgent())
                    .build();

            auditLogRepository.save(auditLog);
            log.debug("Audit log saved: {} - {}", event.getAction(), event.getEntityType());
        } catch (Exception e) {
            log.error("Failed to save audit log: ", e);
        }
    }

    public void publish(
            String action,
            String entityType,
            UUID entityId,
            Map<String, Object> metadata,
            UUID tenantId,
            UUID userId,
            String ipAddress,
            String userAgent
    ) {
        publish(new AuditEvent(action, entityType, entityId, metadata, tenantId, userId, ipAddress, userAgent));
    }

    public void publish(
            String action,
            String entityType,
            UUID entityId,
            Map<String, Object> metadata,
            UUID tenantId,
            UUID userId,
            String ipAddress
    ) {
        publish(new AuditEvent(action, entityType, entityId, metadata, tenantId, userId, ipAddress, null));
    }

    public static class AuditEvent {
        private final String action;
        private final String entityType;
        private final UUID entityId;
        private final Map<String, Object> metadata;
        private final UUID tenantId;
        private final UUID userId;
        private final String ipAddress;
        private final String userAgent;

        public AuditEvent(String action, String entityType, UUID entityId,
                          Map<String, Object> metadata, UUID tenantId, UUID userId,
                          String ipAddress, String userAgent) {
            this.action = action;
            this.entityType = entityType;
            this.entityId = entityId;
            this.metadata = metadata;
            this.tenantId = tenantId;
            this.userId = userId;
            this.ipAddress = ipAddress;
            this.userAgent = userAgent;
        }

        public String getAction() { return action; }
        public String getEntityType() { return entityType; }
        public UUID getEntityId() { return entityId; }
        public Map<String, Object> getMetadata() { return metadata; }
        public UUID getTenantId() { return tenantId; }
        public UUID getUserId() { return userId; }
        public String getIpAddress() { return ipAddress; }
        public String getUserAgent() { return userAgent; }
    }
}