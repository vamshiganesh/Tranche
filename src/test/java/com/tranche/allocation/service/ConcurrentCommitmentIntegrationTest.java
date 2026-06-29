package com.tranche.allocation.service;

import com.tranche.allocation.dto.CommitmentRequest;
import com.tranche.allocation.dto.CommitmentResult;
import com.tranche.allocation.repository.AllocationRepository;
import com.tranche.portfolio.repository.PortfolioPositionRepository;
import com.tranche.allocation.repository.InvestmentOrderRepository;
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
import com.tranche.support.AbstractIntegrationTest;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;

import java.math.BigDecimal;
import java.time.LocalDate;
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

    private Long opportunityId;
    private List<UserPrincipal> investors;

    @BeforeEach
    void setUp() {
        portfolioPositionRepository.deleteAll();
        allocationRepository.deleteAll();
        investmentOrderRepository.deleteAll();
        opportunityRepository.deleteAll();

        Issuer issuer = issuerRepository.findAll().getFirst();
        Opportunity opportunity = new Opportunity();
        opportunity.setIssuer(issuer);
        opportunity.setTitle("Concurrent test invoice");
        opportunity.setFaceValue(new BigDecimal("1000000.0000"));
        opportunity.setDiscountRate(new BigDecimal("8.5000"));
        opportunity.setTenureDays(90);
        opportunity.setMinimumLot(new BigDecimal("10000.0000"));
        opportunity.setRiskGrade(RiskGrade.A);
        opportunity.setTotalUnits(TOTAL_UNITS);
        opportunity.setRemainingUnits(TOTAL_UNITS);
        opportunity.setUnitPrice(new BigDecimal("10000.0000"));
        opportunity.setStatus(OpportunityStatus.LIVE);
        opportunity.setMaturityDate(LocalDate.now().plusDays(90));
        opportunityId = opportunityRepository.save(opportunity).getId();

        investors = userRepository.findAll().stream()
                .filter(user -> user.getRole() == Role.INVESTOR)
                .map(UserPrincipal::from)
                .toList();
        assertThat(investors).hasSizeGreaterThanOrEqualTo(2);
    }

    @Test
    void concurrentCommitmentsNeverOverAllocate() throws Exception {
        ExecutorService executor = Executors.newFixedThreadPool(THREADS);
        CountDownLatch startGate = new CountDownLatch(1);
        List<Future<CommitmentResult>> futures = new ArrayList<>();
        AtomicInteger successes = new AtomicInteger();

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
                    return null;
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
    }
}
