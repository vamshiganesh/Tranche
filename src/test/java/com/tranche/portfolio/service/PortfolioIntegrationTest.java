package com.tranche.portfolio.service;

import com.tranche.allocation.dto.CommitmentRequest;
import com.tranche.allocation.service.AllocationEngine;
import com.tranche.auth.domain.User;
import com.tranche.auth.repository.UserRepository;
import com.tranche.common.domain.Role;
import com.tranche.common.security.UserPrincipal;
import com.tranche.issuer.domain.Issuer;
import com.tranche.issuer.repository.IssuerRepository;
import com.tranche.opportunity.domain.Opportunity;
import com.tranche.opportunity.domain.OpportunityStatus;
import com.tranche.opportunity.domain.RiskGrade;
import com.tranche.opportunity.repository.OpportunityRepository;
import com.tranche.portfolio.dto.PortfolioResponse;
import com.tranche.portfolio.repository.PortfolioPositionRepository;
import com.tranche.support.LocalDatabaseIntegrationTest;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.util.UUID;

import static org.assertj.core.api.Assertions.assertThat;

class PortfolioIntegrationTest extends LocalDatabaseIntegrationTest {

    @Autowired
    private PortfolioService portfolioService;

    @Autowired
    private AllocationEngine allocationEngine;

    @Autowired
    private PortfolioPositionRepository portfolioPositionRepository;

    @Autowired
    private OpportunityRepository opportunityRepository;

    @Autowired
    private IssuerRepository issuerRepository;

    @Autowired
    private UserRepository userRepository;

    private UserPrincipal investor;

    @BeforeEach
    void setUp() {
        portfolioPositionRepository.deleteAll();

        Issuer issuer = issuerRepository.findAll().getFirst();
        Opportunity opportunity = new Opportunity();
        opportunity.setIssuer(issuer);
        opportunity.setTitle("Portfolio test invoice");
        opportunity.setFaceValue(new BigDecimal("1000000.0000"));
        opportunity.setDiscountRate(new BigDecimal("8.5000"));
        opportunity.setTenureDays(90);
        opportunity.setMinimumLot(new BigDecimal("10000.0000"));
        opportunity.setRiskGrade(RiskGrade.A);
        opportunity.setTotalUnits(100);
        opportunity.setRemainingUnits(100);
        opportunity.setUnitPrice(new BigDecimal("10000.0000"));
        opportunity.setStatus(OpportunityStatus.LIVE);
        opportunity.setMaturityDate(LocalDate.now().plusDays(90));
        Long opportunityId = opportunityRepository.save(opportunity).getId();

        User user = userRepository.findAll().stream()
                .filter(u -> u.getRole() == Role.INVESTOR)
                .findFirst()
                .orElseThrow();
        investor = UserPrincipal.from(user);

        allocationEngine.allocate(
                opportunityId,
                UUID.randomUUID(),
                new CommitmentRequest(3, new BigDecimal("30000.0000")),
                investor
        );
    }

    @Test
    void portfolioListsPositionAfterAllocation() {
        PortfolioResponse portfolio = portfolioService.getPortfolio(investor);

        assertThat(portfolio.positions()).hasSize(1);
        assertThat(portfolio.summary().totalInvested()).isEqualByComparingTo("30000.0000");
        assertThat(portfolio.summary().activePositions()).isEqualTo(1);
        assertThat(portfolio.positions().getFirst().investedAmount()).isEqualByComparingTo("30000.0000");
    }
}
