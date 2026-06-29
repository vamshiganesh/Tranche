package com.tranche.opportunity.dto;

import com.tranche.opportunity.domain.ReviewAction;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Size;

public record ReviewOpportunityRequest(
        @NotNull ReviewAction action,
        @Size(max = 1000) String comment
) {
}
