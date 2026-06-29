package com.tranche.opportunity.service;

import com.tranche.allocation.dto.CommitmentRequest;
import com.tranche.allocation.service.AllocationEngine;
import org.springframework.data.domain.Pageable;
import com.tranche.common.exception.InvalidStateTransitionException;
import com.tranche.common.security.UserPrincipal;
import com.tranche.issuer.domain.Issuer;
import com.tranche.issuer.repository.IssuerRepository;
import com.tranche.opportunity.domain.Opportunity;
import com.tranche.opportunity.domain.OpportunityStatus;
import com.tranche.opportunity.domain.ReviewAction;
import com.tranche.opportunity.dto.AdminTransitionRequest;
import com.tranche.opportunity.dto.ReviewOpportunityRequest;
import com.tranche.portfolio.service.PortfolioService;
import com.tranche.support.AbstractIntegrationTest;
import com.tranche.support.OpportunityTestBuilder;
import com.tranche.support.SeedUsers;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.core.context.SecurityContextHolder;

import java.math.BigDecimal;
import java.util.UUID;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

class OpportunityLifecycleIntegrationTest extends AbstractIntegrationTest {

    @Autowired
    private OpportunityService opportunityService;

    @Autowired
    private AllocationEngine allocationEngine;

    @Autowired
    private PortfolioService portfolioService;

    @Autowired
    private IssuerRepository issuerRepository;

    @Autowired
    private AuditLogRepository auditLogRepository;

    private Long opportunityId;

    @BeforeEach
    void setUp() {
        resetInvestorWallets();
        clearTransactionalData();

        Issuer issuer = issuerRepository.findAll().getFirst();
        Opportunity draft = OpportunityTestBuilder.anOpportunity()
                .issuer(issuer)
                .title("Lifecycle integration invoice")
                .totalUnits(20)
                .draft()
                .unitPrice(new BigDecimal("25000.0000"))
                .build();
        opportunityId = opportunityRepository.save(draft).getId();
    }

    @Test
    void fullLifecycleFromDraftToSettled() {
        UserPrincipal issuer = principal(SeedUsers.ISSUER_EMAIL);
        UserPrincipal admin = principal(SeedUsers.ADMIN_EMAIL);
        UserPrincipal investor = principal(SeedUsers.INVESTOR1_EMAIL);

        runAs(issuer, () -> {
            var submitted = opportunityService.submitForReview(opportunityId, issuer);
            assertThat(submitted.status()).isEqualTo(OpportunityStatus.UNDER_REVIEW);
        });

        runAs(admin, () -> {
            var approved = opportunityService.review(
                    opportunityId,
                    new ReviewOpportunityRequest(ReviewAction.APPROVE, "Meets credit policy")
            );
            assertThat(approved.status()).isEqualTo(OpportunityStatus.APPROVED);

            var live = opportunityService.publish(opportunityId);
            assertThat(live.status()).isEqualTo(OpportunityStatus.LIVE);
        });

        allocationEngine.allocate(
                opportunityId,
                UUID.randomUUID(),
                new CommitmentRequest(5, new BigDecimal("125000.0000")),
                investor
        );

        Opportunity afterInvest = opportunityRepository.findById(opportunityId).orElseThrow();
        assertThat(afterInvest.getRemainingUnits()).isEqualTo(15);
        assertThat(portfolioService.getPortfolio(investor).positions()).hasSize(1);

        runAs(admin, () -> {
            var matured = opportunityService.adminTransition(
                    opportunityId,
                    new AdminTransitionRequest(OpportunityStatus.MATURED)
            );
            assertThat(matured.status()).isEqualTo(OpportunityStatus.MATURED);

            var settled = opportunityService.adminTransition(
                    opportunityId,
                    new AdminTransitionRequest(OpportunityStatus.SETTLED)
            );
            assertThat(settled.status()).isEqualTo(OpportunityStatus.SETTLED);
        });

        assertThat(auditLogRepository.findByEntityTypeAndEntityIdOrderByCreatedAtAsc(
                "Opportunity", opportunityId, Pageable.unpaged()).getContent())
                .isNotEmpty();
    }

    @Test
    void invalidTransitionFromDraftToSettledIsRejected() {
        UserPrincipal admin = principal(SeedUsers.ADMIN_EMAIL);

        runAs(admin, () -> assertThatThrownBy(() ->
                opportunityService.adminTransition(
                        opportunityId,
                        new AdminTransitionRequest(OpportunityStatus.SETTLED)
                )
        ).isInstanceOf(InvalidStateTransitionException.class));
    }

    private void runAs(UserPrincipal principal, Runnable action) {
        var authentication = new UsernamePasswordAuthenticationToken(principal, null, principal.getAuthorities());
        SecurityContextHolder.getContext().setAuthentication(authentication);
        try {
            action.run();
        } finally {
            SecurityContextHolder.clearContext();
        }
    }
}
