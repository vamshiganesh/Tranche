package com.tranche.onboarding;

import com.tranche.support.AbstractHttpIntegrationTest;
import com.tranche.support.SeedUsers;
import org.junit.jupiter.api.Test;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;

import java.util.List;
import java.util.Map;
import java.util.UUID;

import static org.assertj.core.api.Assertions.assertThat;

class OnboardingIntegrationTest extends AbstractHttpIntegrationTest {

    @Test
    void investorOnboardingFlow() {
        String email = "new-investor-" + UUID.randomUUID() + "@example.com";
        String password = "Password123!";

        ResponseEntity<Map> register = restTemplate.postForEntity(
                baseUrl() + "/api/v1/auth/register",
                Map.of(
                        "email", email,
                        "password", password,
                        "role", "INVESTOR",
                        "fullName", "New Investor"
                ),
                Map.class
        );
        assertThat(register.getStatusCode()).isEqualTo(HttpStatus.CREATED);
        assertThat(register.getBody()).containsKey("devVerificationCode");

        String code = (String) register.getBody().get("devVerificationCode");
        ResponseEntity<Void> verify = restTemplate.postForEntity(
                baseUrl() + "/api/v1/auth/verify-email",
                Map.of("email", email, "code", code),
                Void.class
        );
        assertThat(verify.getStatusCode()).isEqualTo(HttpStatus.NO_CONTENT);

        String token = loginToken(email, password);
        ResponseEntity<Map> me = restTemplate.exchange(
                baseUrl() + "/api/v1/auth/me",
                org.springframework.http.HttpMethod.GET,
                new org.springframework.http.HttpEntity<>(bearerHeaders(token)),
                Map.class
        );
        assertThat(me.getBody().get("kycStatus")).isEqualTo("PENDING");

        ResponseEntity<Map> credit = postJson(
                "/api/v1/investors/wallet/demo-credit",
                token,
                Map.of(),
                Map.class
        );
        assertThat(credit.getStatusCode()).isEqualTo(HttpStatus.OK);
        assertThat(credit.getBody().get("walletBalance")).isNotNull();

        String adminToken = loginToken(SeedUsers.ADMIN_EMAIL, SeedUsers.PASSWORD);
        ResponseEntity<List> pending = restTemplate.exchange(
                baseUrl() + "/api/v1/admin/onboarding/investors",
                org.springframework.http.HttpMethod.GET,
                new org.springframework.http.HttpEntity<>(bearerHeaders(adminToken)),
                List.class
        );
        assertThat(pending.getStatusCode()).isEqualTo(HttpStatus.OK);
        assertThat(pending.getBody()).isNotEmpty();

        @SuppressWarnings("unchecked")
        Map<String, Object> first = (Map<String, Object>) pending.getBody().stream()
                .filter(row -> email.equals(((Map<?, ?>) row).get("email")))
                .findFirst()
                .orElseThrow();
        String userId = (String) first.get("userId");

        ResponseEntity<Map> approved = postJson(
                "/api/v1/admin/onboarding/investors/" + userId + "/approve",
                adminToken,
                Map.of(),
                Map.class
        );
        assertThat(approved.getStatusCode()).isEqualTo(HttpStatus.OK);
        assertThat(approved.getBody().get("status")).isEqualTo("APPROVED");
    }

    @Test
    void issuerOnboardingFlow() {
        String email = "new-issuer-" + UUID.randomUUID() + "@example.com";
        String password = "Password123!";

        ResponseEntity<Map> register = restTemplate.postForEntity(
                baseUrl() + "/api/v1/auth/register",
                Map.of(
                        "email", email,
                        "password", password,
                        "role", "ISSUER",
                        "fullName", "New Issuer"
                ),
                Map.class
        );
        String code = (String) register.getBody().get("devVerificationCode");
        restTemplate.postForEntity(
                baseUrl() + "/api/v1/auth/verify-email",
                Map.of("email", email, "code", code),
                Void.class
        );

        String token = loginToken(email, password);

        ResponseEntity<Map> blockedCreate = postJson(
                "/api/v1/opportunities",
                token,
                Map.of(
                        "title", "Blocked Invoice",
                        "faceValue", 100000,
                        "discountRate", 0.05,
                        "tenureDays", 90,
                        "minimumLot", 25000,
                        "riskGrade", "B",
                        "totalUnits", 4,
                        "unitPrice", 25000
                ),
                Map.class
        );
        assertThat(blockedCreate.getStatusCode()).isEqualTo(HttpStatus.UNPROCESSABLE_ENTITY);

        postJson(
                "/api/v1/issuers/profile",
                token,
                Map.of("companyName", "Startup Ltd", "registrationNumber", "REG-NEW"),
                Map.class
        );

        String adminToken = loginToken(SeedUsers.ADMIN_EMAIL, SeedUsers.PASSWORD);
        ResponseEntity<List> pending = restTemplate.exchange(
                baseUrl() + "/api/v1/admin/onboarding/issuers",
                org.springframework.http.HttpMethod.GET,
                new org.springframework.http.HttpEntity<>(bearerHeaders(adminToken)),
                List.class
        );
        @SuppressWarnings("unchecked")
        Map<String, Object> row = (Map<String, Object>) pending.getBody().stream()
                .filter(item -> email.equals(((Map<?, ?>) item).get("email")))
                .findFirst()
                .orElseThrow();

        postJson(
                "/api/v1/admin/onboarding/issuers/" + row.get("userId") + "/approve",
                adminToken,
                Map.of(),
                Map.class
        );

        ResponseEntity<Map> created = postJson(
                "/api/v1/opportunities",
                token,
                Map.of(
                        "title", "Approved Invoice",
                        "faceValue", 100000,
                        "discountRate", 0.05,
                        "tenureDays", 90,
                        "minimumLot", 25000,
                        "riskGrade", "B",
                        "totalUnits", 4,
                        "unitPrice", 25000
                ),
                Map.class
        );
        assertThat(created.getStatusCode()).isEqualTo(HttpStatus.CREATED);
    }

    @Test
    void loginBlockedUntilEmailVerified() {
        String email = "unverified-" + UUID.randomUUID() + "@example.com";

        restTemplate.postForEntity(
                baseUrl() + "/api/v1/auth/register",
                Map.of(
                        "email", email,
                        "password", "Password123!",
                        "role", "INVESTOR",
                        "fullName", "Unverified"
                ),
                Map.class
        );

        ResponseEntity<Map> login = restTemplate.postForEntity(
                baseUrl() + "/api/v1/auth/login",
                Map.of("email", email, "password", "Password123!"),
                Map.class
        );
        assertThat(login.getStatusCode()).isEqualTo(HttpStatus.UNPROCESSABLE_ENTITY);
    }
}
