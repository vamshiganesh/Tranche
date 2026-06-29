package com.tranche.audit.dto;

import com.tranche.audit.domain.AuditActorRole;

import java.time.Instant;
import java.util.Map;

public record AuditLogResponse(
        Long id,
        String actorId,
        AuditActorRole actorRole,
        String action,
        String entityType,
        Long entityId,
        Map<String, Object> beforeState,
        Map<String, Object> afterState,
        String correlationId,
        Instant createdAt
) {
}
