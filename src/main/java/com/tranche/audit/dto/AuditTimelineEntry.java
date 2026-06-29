package com.tranche.audit.dto;

import com.tranche.audit.domain.AuditActorRole;

import java.time.Instant;
import java.util.Map;

public record AuditTimelineEntry(
        Long id,
        String action,
        AuditActorRole actorRole,
        String actorId,
        Map<String, Object> beforeState,
        Map<String, Object> afterState,
        String correlationId,
        Instant createdAt
) {
}
