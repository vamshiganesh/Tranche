package com.tranche.notification.controller;

import com.tranche.common.dto.PageResponse;
import com.tranche.notification.domain.OutboxEventStatus;
import com.tranche.notification.dto.OutboxEventResponse;
import com.tranche.notification.dto.OutboxPollResponse;
import com.tranche.notification.service.OutboxPoller;
import org.springframework.data.domain.Pageable;
import org.springframework.data.domain.Sort;
import org.springframework.data.web.PageableDefault;
import org.springframework.http.HttpStatus;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.ResponseStatus;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/api/v1/admin/outbox")
public class OutboxAdminController {

    private final OutboxPoller outboxPoller;

    public OutboxAdminController(OutboxPoller outboxPoller) {
        this.outboxPoller = outboxPoller;
    }

    @GetMapping
    @PreAuthorize("hasRole('ADMIN')")
    public PageResponse<OutboxEventResponse> list(
            @RequestParam(required = false) OutboxEventStatus status,
            @PageableDefault(size = 20, sort = "createdAt", direction = Sort.Direction.DESC) Pageable pageable
    ) {
        return outboxPoller.list(status, pageable);
    }

    @PostMapping("/poll")
    @PreAuthorize("hasRole('ADMIN')")
    @ResponseStatus(HttpStatus.OK)
    public OutboxPollResponse poll() {
        return outboxPoller.pollPendingEvents();
    }
}
