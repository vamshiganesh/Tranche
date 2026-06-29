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
import com.tranche.support.SeedUsers;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;

import java.math.BigDecimal;
import java.util.UUID;

import static org.assertj.core.api.Assertions.assertThat;

class PartialFillIntegrationTest extends AbstractIntegrationTest {

    @Autowired
    private AllocationEngine allocationEngine;

    @Autowired
    private IssuerRepository issuerRepository;

    private Long opportunityId;
    private UserPrincipal investor;

    @BeforeEach
    void setUp() {
        resetInvestorWallets();
        clearTransactionalData();

        Issuer issuer = issuerRepository.findAll().getFirst();
        Opportunity opportunity = OpportunityTestBuilder.anOpportunity()
                .issuer(issuer)
                .title("Partial fill test")
                .totalUnits(100)
                .remainingUnits(10)
                .live()
                .build();
        opportunityId = opportunityRepository.save(opportunity).getId();
        investor = principal(SeedUsers.INVESTOR1_EMAIL);
    }

    @Test
    void requestMoreUnitsThanRemainingReceivesPartialFill() {
        CommitmentRequest request = new CommitmentRequest(15, new BigDecimal("150000.0000"));

        CommitmentResult result = allocationEngine.allocate(
                opportunityId,
                UUID.randomUUID(),
                request,
                investor
        );

        assertThat(result.response().unitsAllocated()).isEqualTo(10);
        assertThat(result.response().fillStatus()).isEqualTo(FillStatus.PARTIAL);
        assertThat(opportunityRepository.findById(opportunityId).orElseThrow().getRemainingUnits()).isZero();
        assertThat(allocationRepository.sumUnitsByOpportunityId(opportunityId)).isEqualTo(10);
    }
}
