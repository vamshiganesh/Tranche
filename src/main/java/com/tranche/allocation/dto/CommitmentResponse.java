package com.tranche.allocation.dto;

import com.tranche.allocation.domain.FillStatus;
import com.tranche.allocation.domain.OrderStatus;

import java.math.BigDecimal;
import java.time.Instant;

public record CommitmentResponse(
        Long orderId,
        Long opportunityId,
        Integer unitsRequested,
        Integer unitsAllocated,
        BigDecimal amountRequested,
        BigDecimal amountAllocated,
        FillStatus fillStatus,
        OrderStatus status,
        String idempotencyKey,
        Instant createdAt
) {
}
