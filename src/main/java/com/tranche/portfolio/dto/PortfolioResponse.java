package com.tranche.portfolio.dto;

import java.util.List;

public record PortfolioResponse(
        PortfolioSummary summary,
        List<PortfolioPositionSummary> positions
) {
}
