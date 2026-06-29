package com.tranche.allocation.controller;

import com.tranche.issuer.domain.Issuer;
import com.tranche.issuer.repository.IssuerRepository;
import com.tranche.opportunity.domain.Opportunity;
import com.tranche.support.AbstractHttpIntegrationTest;
import com.tranche.support.OpportunityTestBuilder;
import com.tranche.support.SeedUsers;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.HttpEntity;
import org.springframework.http.HttpHeaders;
import org.springframework.http.HttpMethod;
import org.springframework.http.HttpStatus;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;

import java.util.Map;
import java.util.UUID;
import java.util.concurrent.CountDownLatch;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.Future;
import java.util.concurrent.atomic.AtomicInteger;

import static org.assertj.core.api.Assertions.assertThat;

class InvestorRaceHttpIntegrationTest extends AbstractHttpIntegrationTest {

    @Autowired
    private IssuerRepository issuerRepository;

    private Long opportunityId;

    @BeforeEach
    void setUp() {
        resetInvestorWallets();
        clearTransactionalData();

        Issuer issuer = issuerRepository.findAll().getFirst();
        Opportunity opportunity = OpportunityTestBuilder.anOpportunity()
                .issuer(issuer)
                .title("Investor race HTTP test")
                .totalUnits(100)
                .remainingUnits(15)
                .unitPrice(new java.math.BigDecimal("10000.0000"))
                .live()
                .build();
        opportunityId = opportunityRepository.save(opportunity).getId();
    }

    @Test
    void twoInvestorsRacingForLastUnitsNeverOverAllocate() throws Exception {
        String investor1Token = loginToken(SeedUsers.INVESTOR1_EMAIL, SeedUsers.PASSWORD);
        String investor2Token = loginToken(SeedUsers.INVESTOR2_EMAIL, SeedUsers.PASSWORD);

        int threads = 10;
        ExecutorService executor = Executors.newFixedThreadPool(threads);
        CountDownLatch startGate = new CountDownLatch(1);
        AtomicInteger successes = new AtomicInteger();

        Future<?>[] futures = new Future[threads];
        for (int i = 0; i < threads; i++) {
            String token = i % 2 == 0 ? investor1Token : investor2Token;
            String idempotencyKey = UUID.randomUUID().toString();
            Map<String, Object> body = Map.of(
                    "unitsRequested", 10,
                    "amount", 100000.0000
            );

            futures[i] = executor.submit(() -> {
                startGate.await();
                HttpHeaders headers = bearerHeaders(token);
                headers.set("Idempotency-Key", idempotencyKey);
                HttpEntity<Map<String, Object>> entity = new HttpEntity<>(body, headers);
                ResponseEntity<Map> response = restTemplate.exchange(
                        baseUrl() + "/api/v1/opportunities/" + opportunityId + "/commitments",
                        HttpMethod.POST,
                        entity,
                        Map.class
                );
                if (response.getStatusCode() == HttpStatus.CREATED || response.getStatusCode() == HttpStatus.OK) {
                    Map<?, ?> payload = response.getBody();
                    if (payload != null && ((Number) payload.get("unitsAllocated")).intValue() > 0) {
                        successes.incrementAndGet();
                    }
                }
                return response;
            });
        }

        startGate.countDown();
        for (Future<?> future : futures) {
            future.get();
        }
        executor.shutdown();

        Opportunity updated = opportunityRepository.findById(opportunityId).orElseThrow();
        int totalAllocated = allocationRepository.sumUnitsByOpportunityId(opportunityId);

        assertThat(totalAllocated).isLessThanOrEqualTo(15);
        assertThat(updated.getRemainingUnits()).isEqualTo(15 - totalAllocated);
        assertThat(totalAllocated + updated.getRemainingUnits()).isEqualTo(15);
        assertThat(successes.get()).isLessThanOrEqualTo(2);
    }
}
