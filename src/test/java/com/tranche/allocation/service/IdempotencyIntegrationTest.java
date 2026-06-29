package com.tranche.allocation.service;

import com.tranche.allocation.dto.CommitmentRequest;
import com.tranche.allocation.dto.CommitmentResult;
import com.tranche.allocation.domain.FillStatus;
import com.tranche.common.security.UserPrincipal;
import com.tranche.issuer.domain.Issuer;
import com.tranche.issuer.repository.IssuerRepository;
import com.tranche.opportunity.domain.Opportunity;
import com.tranche.support.AbstractIntegrationTest;
import com.tranche.support.OpportunityTestBuilder;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;

import java.math.BigDecimal;
import java.util.UUID;

import static org.assertj.core.api.Assertions.assertThat;

class IdempotencyIntegrationTest extends AbstractIntegrationTest {

    @Autowired
    private AllocationEngine allocationEngine;

    @Autowired
    private IssuerRepository issuerRepository;

    private Long opportunityId;
    private UserPrincipal investor;
    private UUID idempotencyKey;

    @BeforeEach
    void setUp() {
        resetInvestorWallets();
        clearTransactionalData();

        Issuer issuer = issuerRepository.findAll().getFirst();
        Opportunity opportunity = OpportunityTestBuilder.anOpportunity()
                .issuer(issuer)
                .title("Idempotency test invoice")
                .live()
                .build();
        opportunityId = opportunityRepository.save(opportunity).getId();

        investor = principal(com.tranche.support.SeedUsers.INVESTOR1_EMAIL);
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
