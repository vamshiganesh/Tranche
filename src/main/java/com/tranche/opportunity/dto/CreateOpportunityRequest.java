package com.tranche.opportunity.dto;

import com.tranche.opportunity.domain.RiskGrade;
import jakarta.validation.constraints.DecimalMax;
import jakarta.validation.constraints.DecimalMin;
import jakarta.validation.constraints.Digits;
import jakarta.validation.constraints.Min;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Positive;
import jakarta.validation.constraints.Size;

import java.math.BigDecimal;

public record CreateOpportunityRequest(
        @NotBlank @Size(max = 500) String title,
        @NotNull @Positive @Digits(integer = 15, fraction = 4) BigDecimal faceValue,
        @NotNull @DecimalMin("0.0") @DecimalMax("100.0") @Digits(integer = 3, fraction = 4) BigDecimal discountRate,
        @NotNull @Min(1) Integer tenureDays,
        @NotNull @Positive @Digits(integer = 15, fraction = 4) BigDecimal minimumLot,
        @NotNull RiskGrade riskGrade,
        @NotNull @Min(1) Integer totalUnits,
        @NotNull @Positive @Digits(integer = 15, fraction = 4) BigDecimal unitPrice,
        @Size(max = 5000) String description,
        Long issuerId
) {
}
