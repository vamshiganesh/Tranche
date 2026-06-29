package com.tranche.opportunity.dto;

import com.tranche.opportunity.domain.OpportunityStatus;
import jakarta.validation.constraints.NotNull;

public record AdminTransitionRequest(
        @NotNull OpportunityStatus targetStatus
) {
}
