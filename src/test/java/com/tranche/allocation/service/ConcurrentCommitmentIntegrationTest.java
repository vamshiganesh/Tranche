package com.tranche.allocation.service;

import com.tranche.allocation.dto.CommitmentRequest;
import com.tranche.allocation.dto.CommitmentResult;
import com.tranche.common.exception.BusinessException;
import com.tranche.common.exception.ErrorCode;
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
import java.util.Set;
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

    private static final Set<ErrorCode> EXPECTED_OVER_SUBSCRIPTION_REJECTIONS = Set.of(
            ErrorCode.OPPORTUNITY_NOT_LIVE,
            ErrorCode.INSUFFICIENT_UNITS,
            ErrorCode.BELOW_MINIMUM_LOT
    );

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

        investors = seededInvestorPrincipals();
        assertThat(investors).hasSize(2);
    }

    @Test
    void concurrentCommitmentsNeverOverAllocate() throws Exception {
        ExecutorService executor = Executors.newFixedThreadPool(THREADS);
        CountDownLatch startGate = new CountDownLatch(1);
        List<Future<?>> futures = new ArrayList<>();
        AtomicInteger allocatedResponses = new AtomicInteger();
        AtomicInteger expectedRejections = new AtomicInteger();
        AtomicInteger unexpectedFailures = new AtomicInteger();

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
                        allocatedResponses.incrementAndGet();
                    }
                    return result;
                } catch (BusinessException ex) {
                    if (EXPECTED_OVER_SUBSCRIPTION_REJECTIONS.contains(ex.getCode())) {
                        expectedRejections.incrementAndGet();
                        return null;
                    }
                    unexpectedFailures.incrementAndGet();
                    throw ex;
                } catch (Exception ex) {
                    unexpectedFailures.incrementAndGet();
                    throw ex;
                }
            }));
        }

        startGate.countDown();

        for (Future<?> future : futures) {
            future.get();
        }
        executor.shutdown();

        Opportunity updated = opportunityRepository.findById(opportunityId).orElseThrow();
        int totalAllocated = allocationRepository.sumUnitsByOpportunityId(opportunityId);

        assertThat(unexpectedFailures.get()).isZero();
        assertThat(totalAllocated).isLessThanOrEqualTo(TOTAL_UNITS);
        assertThat(updated.getRemainingUnits()).isEqualTo(TOTAL_UNITS - totalAllocated);
        assertThat(totalAllocated + updated.getRemainingUnits()).isEqualTo(TOTAL_UNITS);

        long confirmedOrders = investmentOrderRepository.findAll().stream()
                .filter(order -> order.getUnitsAllocated() > 0)
                .count();
        assertThat(confirmedOrders).isEqualTo(allocatedResponses.get());
        assertThat(allocatedResponses.get()).isGreaterThan(0);
        assertThat(allocatedResponses.get() + expectedRejections.get()).isEqualTo(THREADS);
    }
}
