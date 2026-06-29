package com.tranche.portfolio.dto;

import com.tranche.portfolio.domain.PortfolioStatus;

import java.math.BigDecimal;
import java.time.LocalDate;

public record PortfolioPositionSummary(
        Long positionId,
        Long opportunityId,
        String opportunityTitle,
        BigDecimal investedAmount,
        BigDecimal expectedReturn,
        BigDecimal discountRate,
        LocalDate maturityDate,
        PortfolioStatus status,
        BigDecimal realizedYield
) {
}
