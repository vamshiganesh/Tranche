package com.tranche.portfolio.service;

import com.tranche.portfolio.domain.PortfolioPosition;
import com.tranche.portfolio.dto.PortfolioPositionDetail;
import com.tranche.portfolio.dto.PortfolioPositionSummary;
import com.tranche.portfolio.dto.PortfolioResponse;
import com.tranche.portfolio.dto.PortfolioSummary;

public final class PortfolioMapper {

    private PortfolioMapper() {
    }

    public static PortfolioPositionSummary toSummary(PortfolioPosition position) {
        return new PortfolioPositionSummary(
                position.getId(),
                position.getOpportunity().getId(),
                position.getOpportunity().getTitle(),
                position.getInvestedAmount(),
                position.getExpectedReturn(),
                position.getOpportunity().getDiscountRate(),
                position.getMaturityDate(),
                position.getStatus(),
                position.getRealizedYield()
        );
    }

    public static PortfolioPositionDetail toDetail(PortfolioPosition position) {
        return new PortfolioPositionDetail(
                position.getId(),
                position.getOpportunity().getId(),
                position.getOpportunity().getTitle(),
                position.getInvestedAmount(),
                position.getExpectedReturn(),
                position.getOpportunity().getDiscountRate(),
                position.getOpportunity().getTenureDays(),
                position.getMaturityDate(),
                position.getStatus(),
                position.getRealizedYield(),
                position.getAllocation().getId(),
                position.getAllocation().getAllocatedAt()
        );
    }

    public static PortfolioResponse toResponse(
            PortfolioSummary summary,
            java.util.List<PortfolioPositionSummary> positions
    ) {
        return new PortfolioResponse(summary, positions);
    }
}
