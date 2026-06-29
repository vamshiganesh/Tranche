package com.tranche.opportunity.dto;

import com.tranche.opportunity.domain.OpportunityStatus;
import com.tranche.opportunity.domain.RiskGrade;

import java.math.BigDecimal;
import java.time.LocalDate;

public record OpportunitySummaryResponse(
        Long id,
        String title,
        BigDecimal faceValue,
        BigDecimal discountRate,
        Integer tenureDays,
        RiskGrade riskGrade,
        Integer remainingUnits,
        BigDecimal unitPrice,
        OpportunityStatus status,
        LocalDate maturityDate
) {
}
