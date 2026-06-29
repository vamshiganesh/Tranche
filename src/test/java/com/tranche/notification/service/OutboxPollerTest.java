package com.tranche.notification.service;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.tranche.common.config.OutboxProperties;
import com.tranche.notification.domain.OutboxEvent;
import com.tranche.notification.domain.OutboxEventStatus;
import com.tranche.notification.domain.OutboxEventType;
import com.tranche.notification.repository.OutboxEventRepository;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.util.List;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class OutboxPollerTest {

    @Mock
    private OutboxEventRepository outboxEventRepository;

    private OutboxPoller outboxPoller;

    @BeforeEach
    void setUp() {
        outboxPoller = new OutboxPoller(
                outboxEventRepository,
                new OutboxProperties(true, 30_000L, 50),
                new ObjectMapper()
        );
    }

    @Test
    void pollMarksPendingEventsAsPublished() {
        OutboxEvent event = new OutboxEvent();
        event.setEventType(OutboxEventType.INVESTMENT_SUCCESSFUL);
        event.setAggregateType("Allocation");
        event.setAggregateId(1L);
        event.setPayload("{\"units\":10}");
        event.setStatus(OutboxEventStatus.PENDING);

        when(outboxEventRepository.findTop50ByStatusOrderByCreatedAtAsc(OutboxEventStatus.PENDING))
                .thenReturn(List.of(event));
        when(outboxEventRepository.save(any(OutboxEvent.class))).thenAnswer(invocation -> invocation.getArgument(0));

        var result = outboxPoller.pollPendingEvents();

        assertThat(result.published()).isEqualTo(1);
        assertThat(event.getStatus()).isEqualTo(OutboxEventStatus.PUBLISHED);
        assertThat(event.getPublishedAt()).isNotNull();
        verify(outboxEventRepository).save(event);
    }
}
