package com.tranche.portfolio.dto;

import java.math.BigDecimal;

public record PortfolioSummary(
        BigDecimal totalInvested,
        BigDecimal totalExpectedReturn,
        long activePositions,
        BigDecimal realizedYield
) {
}
