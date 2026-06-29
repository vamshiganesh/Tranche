package com.tranche.notification.dto;

import com.tranche.notification.domain.OutboxEventStatus;
import com.tranche.notification.domain.OutboxEventType;

import java.time.Instant;
import java.util.Map;

public record OutboxEventResponse(
        Long id,
        OutboxEventType eventType,
        String aggregateType,
        Long aggregateId,
        Map<String, Object> payload,
        OutboxEventStatus status,
        Integer retryCount,
        Instant createdAt,
        Instant publishedAt
) {
}
