package com.tranche.portfolio.dto;

import com.tranche.portfolio.domain.PortfolioStatus;

import java.math.BigDecimal;
import java.time.Instant;
import java.time.LocalDate;

public record PortfolioPositionDetail(
        Long positionId,
        Long opportunityId,
        String opportunityTitle,
        BigDecimal investedAmount,
        BigDecimal expectedReturn,
        BigDecimal discountRate,
        Integer tenureDays,
        LocalDate maturityDate,
        PortfolioStatus status,
        BigDecimal realizedYield,
        Long allocationId,
        Instant allocatedAt
) {
}
