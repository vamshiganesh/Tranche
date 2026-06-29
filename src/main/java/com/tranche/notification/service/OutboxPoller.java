package com.tranche.notification.service;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.tranche.common.config.OutboxProperties;
import com.tranche.common.dto.PageResponse;
import com.tranche.common.util.JsonMapConverter;
import com.tranche.notification.domain.OutboxEvent;
import com.tranche.notification.domain.OutboxEventStatus;
import com.tranche.notification.dto.OutboxEventResponse;
import com.tranche.notification.dto.OutboxPollResponse;
import com.tranche.notification.repository.OutboxEventRepository;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.Instant;
import java.util.List;
import java.util.Map;

@Service
public class OutboxPoller {

    private static final Logger log = LoggerFactory.getLogger(OutboxPoller.class);

    private final OutboxEventRepository outboxEventRepository;
    private final OutboxProperties outboxProperties;
    private final ObjectMapper objectMapper;

    public OutboxPoller(
            OutboxEventRepository outboxEventRepository,
            OutboxProperties outboxProperties,
            ObjectMapper objectMapper
    ) {
        this.outboxEventRepository = outboxEventRepository;
        this.outboxProperties = outboxProperties;
        this.objectMapper = objectMapper;
    }

    @Scheduled(fixedDelayString = "${tranche.outbox.poll-interval-ms:30000}")
    @Transactional
    public void scheduledPoll() {
        if (!outboxProperties.pollingEnabled()) {
            return;
        }
        pollPendingEvents();
    }

    @Transactional
    public OutboxPollResponse pollPendingEvents() {
        List<OutboxEvent> pending = outboxEventRepository
                .findTop50ByStatusOrderByCreatedAtAsc(OutboxEventStatus.PENDING);

        int published = 0;
        for (OutboxEvent event : pending) {
            if (published >= outboxProperties.batchSize()) {
                break;
            }
            dispatchMock(event);
            published++;
        }
        return new OutboxPollResponse(pending.size(), published);
    }

    @Transactional(readOnly = true)
    public PageResponse<OutboxEventResponse> list(OutboxEventStatus status, Pageable pageable) {
        Page<OutboxEvent> page = status != null
                ? outboxEventRepository.findByStatus(status, pageable)
                : outboxEventRepository.findAllByOrderByCreatedAtDesc(pageable);
        return PageResponse.from(page.map(this::toResponse));
    }

    private void dispatchMock(OutboxEvent event) {
        log.info(
                "Mock notification dispatch: type={} aggregate={}:{} payload={}",
                event.getEventType(),
                event.getAggregateType(),
                event.getAggregateId(),
                event.getPayload()
        );
        event.setStatus(OutboxEventStatus.PUBLISHED);
        event.setPublishedAt(Instant.now());
        outboxEventRepository.save(event);
    }

    private OutboxEventResponse toResponse(OutboxEvent event) {
        return new OutboxEventResponse(
                event.getId(),
                event.getEventType(),
                event.getAggregateType(),
                event.getAggregateId(),
                parsePayload(event.getPayload()),
                event.getStatus(),
                event.getRetryCount(),
                event.getCreatedAt(),
                event.getPublishedAt()
        );
    }

    private Map<String, Object> parsePayload(String json) {
        return JsonMapConverter.fromJson(objectMapper, json);
    }
}
