package com.tranche.opportunity.dto;

import com.tranche.opportunity.domain.RiskGrade;
import jakarta.validation.constraints.DecimalMax;
import jakarta.validation.constraints.DecimalMin;
import jakarta.validation.constraints.Digits;
import jakarta.validation.constraints.Min;
import jakarta.validation.constraints.Positive;
import jakarta.validation.constraints.Size;

import java.math.BigDecimal;

public record UpdateOpportunityRequest(
        @Size(max = 500) String title,
        @Positive @Digits(integer = 15, fraction = 4) BigDecimal faceValue,
        @DecimalMin("0.0") @DecimalMax("100.0") @Digits(integer = 3, fraction = 4) BigDecimal discountRate,
        @Min(1) Integer tenureDays,
        @Positive @Digits(integer = 15, fraction = 4) BigDecimal minimumLot,
        RiskGrade riskGrade,
        @Min(1) Integer totalUnits,
        @Positive @Digits(integer = 15, fraction = 4) BigDecimal unitPrice,
        @Size(max = 5000) String description
) {
}
