package com.tranche.allocation.service;

import com.tranche.allocation.dto.CommitmentRequest;
import com.tranche.allocation.dto.CommitmentResult;
import com.tranche.common.security.UserPrincipal;
import com.tranche.issuer.domain.Issuer;
import com.tranche.issuer.repository.IssuerRepository;
import com.tranche.opportunity.domain.Opportunity;
import com.tranche.opportunity.repository.OpportunityRepository;
import com.tranche.support.AbstractIntegrationTest;
import com.tranche.support.OpportunityTestBuilder;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;

import java.math.BigDecimal;
import java.util.ArrayList;
import java.util.List;
import java.util.UUID;
import java.util.concurrent.CountDownLatch;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.Future;
import java.util.concurrent.atomic.AtomicInteger;

import static org.assertj.core.api.Assertions.assertThat;

class ConcurrentCommitmentIntegrationTest extends AbstractIntegrationTest {

    private static final int TOTAL_UNITS = 100;
    private static final int THREADS = 20;
    private static final int UNITS_PER_REQUEST = 10;

    @Autowired
    private AllocationEngine allocationEngine;

    @Autowired
    private IssuerRepository issuerRepository;

    private Long opportunityId;
    private List<UserPrincipal> investors;

    @BeforeEach
    void setUp() {
        resetInvestorWallets();
        clearTransactionalData();

        Issuer issuer = issuerRepository.findAll().getFirst();
        Opportunity opportunity = OpportunityTestBuilder.anOpportunity()
                .issuer(issuer)
                .title("Concurrent test invoice")
                .totalUnits(TOTAL_UNITS)
                .live()
                .build();
        opportunityId = opportunityRepository.save(opportunity).getId();

        investors = investorPrincipals();
        assertThat(investors).hasSizeGreaterThanOrEqualTo(2);
    }

    @Test
    void concurrentCommitmentsNeverOverAllocate() throws Exception {
        ExecutorService executor = Executors.newFixedThreadPool(THREADS);
        CountDownLatch startGate = new CountDownLatch(1);
        List<Future<CommitmentResult>> futures = new ArrayList<>();
        AtomicInteger successes = new AtomicInteger();
        AtomicInteger failures = new AtomicInteger();

        for (int i = 0; i < THREADS; i++) {
            UserPrincipal investor = investors.get(i % investors.size());
            UUID idempotencyKey = UUID.randomUUID();
            CommitmentRequest request = new CommitmentRequest(
                    UNITS_PER_REQUEST,
                    new BigDecimal("100000.0000")
            );

            futures.add(executor.submit(() -> {
                startGate.await();
                try {
                    CommitmentResult result = allocationEngine.allocate(
                            opportunityId,
                            idempotencyKey,
                            request,
                            investor
                    );
                    if (result.response().unitsAllocated() > 0) {
                        successes.incrementAndGet();
                    }
                    return result;
                } catch (Exception ex) {
                    failures.incrementAndGet();
                    throw ex;
                }
            }));
        }

        startGate.countDown();

        for (Future<CommitmentResult> future : futures) {
            future.get();
        }
        executor.shutdown();

        Opportunity updated = opportunityRepository.findById(opportunityId).orElseThrow();
        int totalAllocated = allocationRepository.sumUnitsByOpportunityId(opportunityId);

        assertThat(totalAllocated).isLessThanOrEqualTo(TOTAL_UNITS);
        assertThat(updated.getRemainingUnits()).isEqualTo(TOTAL_UNITS - totalAllocated);
        assertThat(totalAllocated + updated.getRemainingUnits()).isEqualTo(TOTAL_UNITS);

        long confirmedOrders = investmentOrderRepository.findAll().stream()
                .filter(order -> order.getUnitsAllocated() > 0)
                .count();
        assertThat(confirmedOrders).isEqualTo(successes.get());
        assertThat(failures.get()).isZero();
    }
}
