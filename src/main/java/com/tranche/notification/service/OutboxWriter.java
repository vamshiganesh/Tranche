package com.tranche.notification.service;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.tranche.common.util.JsonMapConverter;
import com.tranche.notification.domain.OutboxEvent;
import com.tranche.notification.domain.OutboxEventStatus;
import com.tranche.notification.domain.OutboxEventType;
import com.tranche.notification.repository.OutboxEventRepository;
import org.springframework.stereotype.Service;

import java.util.Map;

@Service
public class OutboxWriter {

    private final OutboxEventRepository outboxEventRepository;
    private final ObjectMapper objectMapper;

    public OutboxWriter(OutboxEventRepository outboxEventRepository, ObjectMapper objectMapper) {
        this.outboxEventRepository = outboxEventRepository;
        this.objectMapper = objectMapper;
    }

    /** Participates in the caller's transaction (no @Transactional here). */
    public void write(
            OutboxEventType eventType,
            String aggregateType,
            Long aggregateId,
            Map<String, Object> payload
    ) {
        OutboxEvent event = new OutboxEvent();
        event.setEventType(eventType);
        event.setAggregateType(aggregateType);
        event.setAggregateId(aggregateId);
        event.setPayload(JsonMapConverter.toJsonRequired(objectMapper, payload));
        event.setStatus(OutboxEventStatus.PENDING);
        outboxEventRepository.save(event);
    }
}
