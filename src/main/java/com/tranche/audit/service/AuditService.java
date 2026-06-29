package com.tranche.audit.service;

import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.tranche.audit.domain.AuditActorRole;
import com.tranche.audit.domain.AuditLog;
import com.tranche.audit.repository.AuditLogRepository;
import com.tranche.auth.domain.User;
import com.tranche.common.util.CorrelationIdHolder;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Service;

import java.util.Map;

@Service
public class AuditService {

    private static final Logger log = LoggerFactory.getLogger(AuditService.class);

    private final AuditLogRepository auditLogRepository;
    private final ObjectMapper objectMapper;

    public AuditService(AuditLogRepository auditLogRepository, ObjectMapper objectMapper) {
        this.auditLogRepository = auditLogRepository;
        this.objectMapper = objectMapper;
    }

    public void log(
            User actor,
            AuditActorRole actorRole,
            String action,
            String entityType,
            Long entityId,
            Map<String, Object> beforeState,
            Map<String, Object> afterState
    ) {
        AuditLog entry = new AuditLog();
        entry.setActor(actor);
        entry.setActorRole(actorRole);
        entry.setAction(action);
        entry.setEntityType(entityType);
        entry.setEntityId(entityId);
        entry.setBeforeState(toJson(beforeState));
        entry.setAfterState(toJson(afterState));
        entry.setCorrelationId(CorrelationIdHolder.get());
        auditLogRepository.save(entry);
    }

    private String toJson(Map<String, Object> state) {
        if (state == null || state.isEmpty()) {
            return null;
        }
        try {
            return objectMapper.writeValueAsString(state);
        } catch (JsonProcessingException ex) {
            log.warn("Failed to serialize audit state", ex);
            return null;
        }
    }
}
