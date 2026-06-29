package com.tranche.portfolio.service;

import com.tranche.audit.domain.AuditActions;
import com.tranche.audit.domain.AuditActorRole;
import com.tranche.audit.service.AuditService;
import com.tranche.auth.domain.User;
import com.tranche.common.exception.ResourceNotFoundException;
import com.tranche.common.security.UserPrincipal;
import com.tranche.opportunity.domain.Opportunity;
import com.tranche.opportunity.domain.OpportunityStatus;
import com.tranche.opportunity.repository.OpportunityRepository;
import com.tranche.portfolio.domain.PortfolioPosition;
import com.tranche.portfolio.domain.PortfolioStatus;
import com.tranche.portfolio.domain.RealizedYieldCalculator;
import com.tranche.portfolio.dto.PortfolioPositionDetail;
import com.tranche.portfolio.dto.PortfolioResponse;
import com.tranche.portfolio.dto.PortfolioSummary;
import com.tranche.portfolio.repository.PortfolioPositionRepository;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.math.BigDecimal;
import java.util.List;
import java.util.Map;

@Service
public class PortfolioService {

    private final PortfolioPositionRepository portfolioPositionRepository;
    private final OpportunityRepository opportunityRepository;
    private final AuditService auditService;

    public PortfolioService(
            PortfolioPositionRepository portfolioPositionRepository,
            OpportunityRepository opportunityRepository,
            AuditService auditService
    ) {
        this.portfolioPositionRepository = portfolioPositionRepository;
        this.opportunityRepository = opportunityRepository;
        this.auditService = auditService;
    }

    @Transactional(readOnly = true)
    public PortfolioResponse getPortfolio(UserPrincipal investor) {
        List<PortfolioPosition> positions = portfolioPositionRepository
                .findAllByInvestorIdWithDetails(investor.getId());

        BigDecimal totalInvested = BigDecimal.ZERO;
        BigDecimal totalExpectedReturn = BigDecimal.ZERO;
        BigDecimal settledYieldSum = BigDecimal.ZERO;
        long settledCount = 0;

        for (PortfolioPosition position : positions) {
            totalInvested = totalInvested.add(position.getInvestedAmount());
            totalExpectedReturn = totalExpectedReturn.add(position.getExpectedReturn());
            if (position.getStatus() == PortfolioStatus.SETTLED && position.getRealizedYield() != null) {
                settledYieldSum = settledYieldSum.add(position.getRealizedYield());
                settledCount++;
            }
        }

        long activePositions = portfolioPositionRepository.countByInvestor_IdAndStatus(
                investor.getId(),
                PortfolioStatus.ACTIVE
        );

        BigDecimal portfolioRealizedYield = settledCount > 0
                ? settledYieldSum.divide(BigDecimal.valueOf(settledCount), 4, java.math.RoundingMode.HALF_UP)
                : null;

        PortfolioSummary summary = new PortfolioSummary(
                totalInvested,
                totalExpectedReturn,
                activePositions,
                portfolioRealizedYield
        );

        List<com.tranche.portfolio.dto.PortfolioPositionSummary> summaries = positions.stream()
                .map(PortfolioMapper::toSummary)
                .toList();

        return PortfolioMapper.toResponse(summary, summaries);
    }

    @Transactional(readOnly = true)
    public PortfolioPositionDetail getPosition(Long positionId, UserPrincipal investor) {
        PortfolioPosition position = portfolioPositionRepository
                .findByIdAndInvestorIdWithDetails(positionId, investor.getId())
                .orElseThrow(() -> new ResourceNotFoundException("Portfolio position not found"));
        return PortfolioMapper.toDetail(position);
    }

    /**
     * Called when an opportunity transitions to MATURED — marks linked positions as matured.
     */
    @Transactional
    public void maturePositionsForOpportunity(Long opportunityId, User actor) {
        List<PortfolioPosition> positions = portfolioPositionRepository.findByOpportunity_Id(opportunityId);
        for (PortfolioPosition position : positions) {
            if (position.getStatus() != PortfolioStatus.ACTIVE) {
                continue;
            }
            PortfolioStatus before = position.getStatus();
            position.setStatus(PortfolioStatus.MATURED);
            portfolioPositionRepository.save(position);
            auditService.log(
                    actor,
                    AuditActorRole.SYSTEM,
                    AuditActions.PORTFOLIO_POSITION_MATURED,
                    "PortfolioPosition",
                    position.getId(),
                    Map.of("status", before.name()),
                    Map.of("status", PortfolioStatus.MATURED.name(), "opportunityId", opportunityId)
            );
        }
    }

    /**
     * Called when an opportunity transitions to SETTLED — computes realized yield on each position.
     */
    @Transactional
    public void settlePositionsForOpportunity(Long opportunityId, User actor) {
        Opportunity opportunity = opportunityRepository.findById(opportunityId)
                .orElseThrow(() -> new ResourceNotFoundException("Opportunity not found"));

        List<PortfolioPosition> positions = portfolioPositionRepository.findByOpportunity_Id(opportunityId);
        for (PortfolioPosition position : positions) {
            if (position.getStatus() == PortfolioStatus.SETTLED) {
                continue;
            }
            PortfolioStatus before = position.getStatus();
            BigDecimal realizedYield = RealizedYieldCalculator.annualizedPercent(
                    position.getInvestedAmount(),
                    position.getExpectedReturn(),
                    opportunity.getTenureDays()
            );
            position.setRealizedYield(realizedYield);
            position.setStatus(PortfolioStatus.SETTLED);
            portfolioPositionRepository.save(position);

            auditService.log(
                    actor,
                    AuditActorRole.SYSTEM,
                    AuditActions.PORTFOLIO_POSITION_SETTLED,
                    "PortfolioPosition",
                    position.getId(),
                    Map.of("status", before.name(), "realizedYield", (Object) null),
                    Map.of(
                            "status", PortfolioStatus.SETTLED.name(),
                            "realizedYield", realizedYield,
                            "opportunityId", opportunityId
                    )
            );
        }
    }
}
