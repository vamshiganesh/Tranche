package com.tranche.support;

import com.tranche.opportunity.domain.OpportunityStatus;
import org.junit.jupiter.api.Test;

import static org.assertj.core.api.Assertions.assertThat;

/**
 * Verifies Flyway seed migrations on a fresh Testcontainers database.
 */
class FlywaySeedDataIntegrationTest extends AbstractIntegrationTest {

    @Test
    void seededUsersExist() {
        assertThat(requireUser(SeedUsers.ADMIN_EMAIL).getRole().name()).isEqualTo("ADMIN");
        assertThat(requireUser(SeedUsers.ISSUER_EMAIL).getRole().name()).isEqualTo("ISSUER");
        assertThat(investorPrincipals()).hasSizeGreaterThanOrEqualTo(2);
    }

    @Test
    void seededDemoOpportunityIsDraft() {
        var demo = opportunityRepository.findAll().stream()
                .filter(o -> SeedUsers.DEMO_OPPORTUNITY_TITLE.equals(o.getTitle()))
                .findFirst()
                .orElseThrow();
        assertThat(demo.getStatus()).isEqualTo(OpportunityStatus.DRAFT);
        assertThat(demo.getTotalUnits()).isEqualTo(100);
        assertThat(demo.getRemainingUnits()).isEqualTo(100);
    }
}
