package com.tranche.allocation.service;

import com.tranche.allocation.dto.CommitmentRequest;
import com.tranche.allocation.dto.CommitmentResult;
import com.tranche.allocation.repository.AllocationRepository;
import com.tranche.portfolio.repository.PortfolioPositionRepository;
import com.tranche.allocation.repository.InvestmentOrderRepository;
import com.tranche.audit.repository.AuditLogRepository;
import com.tranche.auth.domain.User;
import com.tranche.auth.repository.UserRepository;
import com.tranche.common.domain.Role;
import com.tranche.common.security.UserPrincipal;
import com.tranche.issuer.domain.Issuer;
import com.tranche.issuer.repository.IssuerRepository;
import com.tranche.notification.repository.OutboxEventRepository;
import com.tranche.opportunity.domain.Opportunity;
import com.tranche.opportunity.domain.OpportunityStatus;
import com.tranche.opportunity.domain.RiskGrade;
import com.tranche.opportunity.repository.OpportunityRepository;
import com.tranche.support.AbstractIntegrationTest;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.util.UUID;

import static org.assertj.core.api.Assertions.assertThat;

class IdempotencyIntegrationTest extends AbstractIntegrationTest {

    @Autowired
    private AllocationEngine allocationEngine;

    @Autowired
    private OpportunityRepository opportunityRepository;

    @Autowired
    private IssuerRepository issuerRepository;

    @Autowired
    private UserRepository userRepository;

    @Autowired
    private InvestmentOrderRepository investmentOrderRepository;

    @Autowired
    private PortfolioPositionRepository portfolioPositionRepository;

    @Autowired
    private AllocationRepository allocationRepository;

    @Autowired
    private AuditLogRepository auditLogRepository;

    @Autowired
    private OutboxEventRepository outboxEventRepository;

    private Long opportunityId;
    private UserPrincipal investor;
    private UUID idempotencyKey;

    @BeforeEach
    void setUp() {
        portfolioPositionRepository.deleteAll();
        allocationRepository.deleteAll();
        investmentOrderRepository.deleteAll();
        auditLogRepository.deleteAll();
        outboxEventRepository.deleteAll();
        opportunityRepository.deleteAll();

        Issuer issuer = issuerRepository.findAll().getFirst();
        Opportunity opportunity = new Opportunity();
        opportunity.setIssuer(issuer);
        opportunity.setTitle("Idempotency test invoice");
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
        opportunityId = opportunityRepository.save(opportunity).getId();

        User user = userRepository.findAll().stream()
                .filter(u -> u.getRole() == Role.INVESTOR)
                .findFirst()
                .orElseThrow();
        investor = UserPrincipal.from(user);
        idempotencyKey = UUID.randomUUID();
    }

    @Test
    void duplicateIdempotencyKeyReturnsSameOrderWithoutDoubleAllocation() {
        CommitmentRequest request = new CommitmentRequest(5, new BigDecimal("50000.0000"));

        CommitmentResult first = allocationEngine.allocate(opportunityId, idempotencyKey, request, investor);
        CommitmentResult second = allocationEngine.allocate(opportunityId, idempotencyKey, request, investor);

        assertThat(first.replay()).isFalse();
        assertThat(second.replay()).isTrue();
        assertThat(second.response().orderId()).isEqualTo(first.response().orderId());
        assertThat(second.response().unitsAllocated()).isEqualTo(5);

        assertThat(investmentOrderRepository.count()).isEqualTo(1);
        assertThat(allocationRepository.count()).isEqualTo(1);
        assertThat(allocationRepository.sumUnitsByOpportunityId(opportunityId)).isEqualTo(5);

        Opportunity updated = opportunityRepository.findById(opportunityId).orElseThrow();
        assertThat(updated.getRemainingUnits()).isEqualTo(95);
    }

    @Test
    void successfulCommitmentWritesAuditAndOutbox() {
        CommitmentRequest request = new CommitmentRequest(2, new BigDecimal("20000.0000"));

        allocationEngine.allocate(opportunityId, idempotencyKey, request, investor);

        assertThat(auditLogRepository.count()).isGreaterThanOrEqualTo(2);
        assertThat(outboxEventRepository.count()).isEqualTo(1);
        assertThat(outboxEventRepository.findAll().getFirst().getEventType().name())
                .isEqualTo("INVESTMENT_SUCCESSFUL");
    }
}
