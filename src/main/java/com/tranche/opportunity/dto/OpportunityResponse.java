package com.tranche.opportunity.dto;

import com.tranche.opportunity.domain.OpportunityStatus;
import com.tranche.opportunity.domain.RiskGrade;

import java.math.BigDecimal;
import java.time.Instant;
import java.time.LocalDate;

public record OpportunityResponse(
        Long id,
        String title,
        BigDecimal faceValue,
        BigDecimal discountRate,
        Integer tenureDays,
        BigDecimal minimumLot,
        RiskGrade riskGrade,
        Integer totalUnits,
        Integer remainingUnits,
        BigDecimal unitPrice,
        OpportunityStatus status,
        String description,
        Long issuerId,
        String issuerName,
        LocalDate maturityDate,
        Instant createdAt,
        Instant updatedAt,
        Instant publishedAt,
        Instant reviewedAt,
        String reviewComment
) {
}
