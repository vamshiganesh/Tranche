package com.tranche.audit.service;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.tranche.audit.domain.AuditActorRole;
import com.tranche.audit.domain.AuditLog;
import com.tranche.audit.dto.AuditLogResponse;
import com.tranche.audit.dto.AuditTimelineEntry;
import com.tranche.audit.dto.AuditTimelineResponse;
import com.tranche.audit.repository.AuditLogRepository;
import com.tranche.auth.domain.User;
import com.tranche.common.dto.PageResponse;
import com.tranche.common.util.CorrelationIdHolder;
import com.tranche.common.util.JsonMapConverter;
import com.tranche.opportunity.domain.OpportunityStatus;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;
import java.util.Map;

@Service
public class AuditService {

    private final AuditLogRepository auditLogRepository;
    private final ObjectMapper objectMapper;

    public AuditService(AuditLogRepository auditLogRepository, ObjectMapper objectMapper) {
        this.auditLogRepository = auditLogRepository;
        this.objectMapper = objectMapper;
    }

    /**
     * Append-only write — participates in the caller's transaction (no @Transactional here).
     */
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
        entry.setBeforeState(JsonMapConverter.toJson(objectMapper, beforeState));
        entry.setAfterState(JsonMapConverter.toJson(objectMapper, afterState));
        entry.setCorrelationId(CorrelationIdHolder.get());
        auditLogRepository.save(entry);
    }

    public void logStatusTransition(
            User actor,
            AuditActorRole actorRole,
            String action,
            String entityType,
            Long entityId,
            OpportunityStatus from,
            OpportunityStatus to
    ) {
        log(
                actor,
                actorRole,
                action,
                entityType,
                entityId,
                Map.of("status", from.name()),
                Map.of("status", to.name())
        );
    }

    @Transactional(readOnly = true)
    public PageResponse<AuditLogResponse> list(String entityType, Long entityId, Pageable pageable) {
        Page<AuditLog> page = auditLogRepository.findFiltered(entityType, entityId, pageable);
        return PageResponse.from(page.map(this::toResponse));
    }

    @Transactional(readOnly = true)
    public AuditTimelineResponse getTimeline(String entityType, Long entityId) {
        List<AuditLog> entries = auditLogRepository.findTimelineByEntity(entityType, entityId);
        List<AuditTimelineEntry> timeline = entries.stream().map(this::toTimelineEntry).toList();
        return new AuditTimelineResponse(entityType, entityId, timeline);
    }

    private AuditLogResponse toResponse(AuditLog entry) {
        return new AuditLogResponse(
                entry.getId(),
                actorPublicId(entry),
                entry.getActorRole(),
                entry.getAction(),
                entry.getEntityType(),
                entry.getEntityId(),
                parseJson(entry.getBeforeState()),
                parseJson(entry.getAfterState()),
                entry.getCorrelationId(),
                entry.getCreatedAt()
        );
    }

    private AuditTimelineEntry toTimelineEntry(AuditLog entry) {
        return new AuditTimelineEntry(
                entry.getId(),
                entry.getAction(),
                entry.getActorRole(),
                actorPublicId(entry),
                parseJson(entry.getBeforeState()),
                parseJson(entry.getAfterState()),
                entry.getCorrelationId(),
                entry.getCreatedAt()
        );
    }

    private String actorPublicId(AuditLog entry) {
        if (entry.getActor() == null) {
            return null;
        }
        return entry.getActor().getPublicId().toString();
    }

    private Map<String, Object> parseJson(String json) {
        if (json == null || json.isBlank()) {
            return null;
        }
        return JsonMapConverter.fromJson(objectMapper, json);
    }
}
