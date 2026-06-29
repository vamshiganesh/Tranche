package com.tranche.allocation.service;

import com.tranche.allocation.dto.CommitmentRequest;
import com.tranche.allocation.dto.CommitmentResult;
import com.tranche.allocation.domain.FillStatus;
import com.tranche.common.security.UserPrincipal;
import com.tranche.support.AbstractIntegrationTest;
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

    private Long opportunityId;
    private UserPrincipal investor;

    @BeforeEach
    void setUp() {
        opportunityId = prepareLiveOpportunity("Partial fill test", 100, 10);
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
