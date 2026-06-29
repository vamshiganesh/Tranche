package com.tranche.allocation.controller;

import com.fasterxml.jackson.databind.JsonNode;
import com.tranche.issuer.domain.Issuer;
import com.tranche.issuer.repository.IssuerRepository;
import com.tranche.opportunity.domain.Opportunity;
import com.tranche.support.AbstractMockMvcIntegrationTest;
import com.tranche.support.OpportunityTestBuilder;
import com.tranche.support.SeedUsers;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.MediaType;

import java.util.UUID;

import static org.assertj.core.api.Assertions.assertThat;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

class CommitmentHttpIntegrationTest extends AbstractMockMvcIntegrationTest {

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
                .title("HTTP commitment test")
                .live()
                .build();
        opportunityId = opportunityRepository.save(opportunity).getId();
    }

    @Test
    void duplicateIdempotencyKeyReturns200OnReplay() throws Exception {
        String token = loginToken(SeedUsers.INVESTOR1_EMAIL, SeedUsers.PASSWORD);
        String idempotencyKey = UUID.randomUUID().toString();
        String body = """
                {"unitsRequested": 3, "amount": 30000.0000}
                """;

        mockMvc.perform(post("/api/v1/opportunities/" + opportunityId + "/commitments")
                        .header("Authorization", "Bearer " + token)
                        .header("Idempotency-Key", idempotencyKey)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(body))
                .andExpect(status().isCreated());

        String replayBody = mockMvc.perform(post("/api/v1/opportunities/" + opportunityId + "/commitments")
                        .header("Authorization", "Bearer " + token)
                        .header("Idempotency-Key", idempotencyKey)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(body))
                .andExpect(status().isOk())
                .andReturn()
                .getResponse()
                .getContentAsString();

        JsonNode json = objectMapper.readTree(replayBody);
        assertThat(investmentOrderRepository.count()).isEqualTo(1);
        assertThat(json.get("unitsAllocated").asInt()).isEqualTo(3);
    }

    @Test
    void missingIdempotencyKeyReturns400() throws Exception {
        String token = loginToken(SeedUsers.INVESTOR1_EMAIL, SeedUsers.PASSWORD);
        String body = """
                {"unitsRequested": 1, "amount": 10000.0000}
                """;

        mockMvc.perform(post("/api/v1/opportunities/" + opportunityId + "/commitments")
                        .header("Authorization", "Bearer " + token)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(body))
                .andExpect(status().isBadRequest());
    }
}
