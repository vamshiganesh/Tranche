package com.tranche.opportunity.service;

import com.tranche.opportunity.domain.Opportunity;
import com.tranche.opportunity.dto.OpportunityResponse;
import com.tranche.opportunity.dto.OpportunityStatusResponse;
import com.tranche.opportunity.dto.OpportunitySummaryResponse;

public final class OpportunityMapper {

    private OpportunityMapper() {
    }

    public static OpportunityResponse toResponse(Opportunity opportunity) {
        return new OpportunityResponse(
                opportunity.getId(),
                opportunity.getTitle(),
                opportunity.getFaceValue(),
                opportunity.getDiscountRate(),
                opportunity.getTenureDays(),
                opportunity.getMinimumLot(),
                opportunity.getRiskGrade(),
                opportunity.getTotalUnits(),
                opportunity.getRemainingUnits(),
                opportunity.getUnitPrice(),
                opportunity.getStatus(),
                opportunity.getDescription(),
                opportunity.getIssuer().getId(),
                opportunity.getIssuer().getCompanyName(),
                opportunity.getMaturityDate(),
                opportunity.getCreatedAt(),
                opportunity.getUpdatedAt(),
                opportunity.getPublishedAt(),
                opportunity.getReviewedAt(),
                opportunity.getReviewComment()
        );
    }

    public static OpportunitySummaryResponse toSummary(Opportunity opportunity) {
        return new OpportunitySummaryResponse(
                opportunity.getId(),
                opportunity.getTitle(),
                opportunity.getFaceValue(),
                opportunity.getDiscountRate(),
                opportunity.getTenureDays(),
                opportunity.getRiskGrade(),
                opportunity.getRemainingUnits(),
                opportunity.getUnitPrice(),
                opportunity.getStatus(),
                opportunity.getMaturityDate()
        );
    }

    public static OpportunityStatusResponse toStatusResponse(Opportunity opportunity) {
        return new OpportunityStatusResponse(
                opportunity.getId(),
                opportunity.getStatus(),
                opportunity.getUpdatedAt(),
                opportunity.getReviewedAt(),
                opportunity.getPublishedAt(),
                opportunity.getReviewComment()
        );
    }
}
