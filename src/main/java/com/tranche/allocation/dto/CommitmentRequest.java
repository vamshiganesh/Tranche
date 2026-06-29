package com.tranche.allocation.dto;

import jakarta.validation.constraints.DecimalMin;
import jakarta.validation.constraints.Max;
import jakarta.validation.constraints.Min;
import jakarta.validation.constraints.NotNull;

import java.math.BigDecimal;

public record CommitmentRequest(
        @NotNull @Min(1) @Max(100_000) Integer unitsRequested,
        @NotNull @DecimalMin(value = "0.0001") BigDecimal amount
) {
}
