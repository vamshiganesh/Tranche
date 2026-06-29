package com.tranche.audit.dto;

import java.util.List;

public record AuditTimelineResponse(
        String entityType,
        Long entityId,
        List<AuditTimelineEntry> timeline
) {
}
