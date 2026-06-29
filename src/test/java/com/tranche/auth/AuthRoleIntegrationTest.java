package com.tranche.auth;

import com.tranche.support.AbstractMockMvcIntegrationTest;
import com.tranche.support.SeedUsers;
import org.junit.jupiter.api.Test;
import org.springframework.http.MediaType;

import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

class AuthRoleIntegrationTest extends AbstractMockMvcIntegrationTest {

    @Test
    void unauthenticatedRequestReturns401() throws Exception {
        mockMvc.perform(get("/api/v1/portfolio"))
                .andExpect(status().isUnauthorized());
    }

    @Test
    void investorCannotAccessAuditApi() throws Exception {
        String token = loginToken(SeedUsers.INVESTOR1_EMAIL, SeedUsers.PASSWORD);

        mockMvc.perform(get("/api/v1/audit")
                        .header("Authorization", "Bearer " + token))
                .andExpect(status().isForbidden());
    }

    @Test
    void investorCannotCreateOpportunity() throws Exception {
        String token = loginToken(SeedUsers.INVESTOR1_EMAIL, SeedUsers.PASSWORD);
        String body = """
                {
                  "title": "Unauthorized invoice",
                  "faceValue": 100000.0000,
                  "discountRate": 5.0,
                  "tenureDays": 60,
                  "minimumLot": 10000.0000,
                  "riskGrade": "B",
                  "totalUnits": 10,
                  "unitPrice": 10000.0000
                }
                """;

        mockMvc.perform(post("/api/v1/opportunities")
                        .header("Authorization", "Bearer " + token)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(body))
                .andExpect(status().isForbidden());
    }

    @Test
    void issuerCannotPlaceCommitment() throws Exception {
        String token = loginToken(SeedUsers.ISSUER_EMAIL, SeedUsers.PASSWORD);
        String body = """
                {"unitsRequested": 1, "amount": 25000.0000}
                """;

        mockMvc.perform(post("/api/v1/opportunities/1/commitments")
                        .header("Authorization", "Bearer " + token)
                        .header("Idempotency-Key", "test-key-issuer-blocked")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(body))
                .andExpect(status().isForbidden());
    }

    @Test
    void adminCannotPlaceCommitment() throws Exception {
        String token = loginToken(SeedUsers.ADMIN_EMAIL, SeedUsers.PASSWORD);
        String body = """
                {"unitsRequested": 1, "amount": 25000.0000}
                """;

        mockMvc.perform(post("/api/v1/opportunities/1/commitments")
                        .header("Authorization", "Bearer " + token)
                        .header("Idempotency-Key", "test-key-admin-blocked")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(body))
                .andExpect(status().isForbidden());
    }

    @Test
    void investorCanAccessPortfolio() throws Exception {
        String token = loginToken(SeedUsers.INVESTOR1_EMAIL, SeedUsers.PASSWORD);

        mockMvc.perform(get("/api/v1/portfolio")
                        .header("Authorization", "Bearer " + token))
                .andExpect(status().isOk());
    }
}
