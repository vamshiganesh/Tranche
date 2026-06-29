package com.tranche.opportunity.dto;

import com.tranche.opportunity.domain.OpportunityStatus;

import java.time.Instant;

public record OpportunityStatusResponse(
        Long id,
        OpportunityStatus status,
        Instant updatedAt,
        Instant reviewedAt,
        Instant publishedAt,
        String reviewComment
) {
}
