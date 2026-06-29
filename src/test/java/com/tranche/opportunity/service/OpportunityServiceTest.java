package com.tranche.opportunity.service;

import com.tranche.audit.service.AuditService;
import com.tranche.auth.domain.User;
import com.tranche.auth.repository.UserRepository;
import com.tranche.common.domain.Role;
import com.tranche.common.exception.BusinessException;
import com.tranche.common.exception.InvalidStateTransitionException;
import com.tranche.common.security.SecurityUtils;
import com.tranche.common.security.UserPrincipal;
import com.tranche.issuer.domain.Issuer;
import com.tranche.issuer.repository.IssuerRepository;
import com.tranche.notification.service.OutboxWriter;
import com.tranche.opportunity.domain.Opportunity;
import com.tranche.opportunity.domain.OpportunityStateMachine;
import com.tranche.opportunity.domain.OpportunityStatus;
import com.tranche.opportunity.domain.ReviewAction;
import com.tranche.opportunity.domain.RiskGrade;
import com.tranche.opportunity.dto.AdminTransitionRequest;
import com.tranche.opportunity.dto.ReviewOpportunityRequest;
import com.tranche.opportunity.repository.OpportunityRepository;
import com.tranche.portfolio.service.PortfolioService;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.MockedStatic;
import org.mockito.Mockito;
import org.mockito.junit.jupiter.MockitoExtension;

import java.math.BigDecimal;
import java.util.Optional;
import java.util.UUID;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyLong;
import static org.mockito.Mockito.lenient;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class OpportunityServiceTest {

    @Mock
    private OpportunityRepository opportunityRepository;
    @Mock
    private IssuerRepository issuerRepository;
    @Mock
    private AuditService auditService;
    @Mock
    private OutboxWriter outboxWriter;
    @Mock
    private PortfolioService portfolioService;
    @Mock
    private UserRepository userRepository;

    private OpportunityService opportunityService;
    private MockedStatic<SecurityUtils> securityUtils;

    @BeforeEach
    void setUp() {
        opportunityService = new OpportunityService(
                opportunityRepository,
                issuerRepository,
                new OpportunityStateMachine(),
                auditService,
                outboxWriter,
                portfolioService,
                userRepository
        );
        securityUtils = Mockito.mockStatic(SecurityUtils.class);
        securityUtils.when(SecurityUtils::requireCurrentUser).thenReturn(adminPrincipal());
        lenient().when(userRepository.getReferenceById(anyLong())).thenReturn(new User());
    }

    @AfterEach
    void tearDown() {
        securityUtils.close();
    }

    @Test
    void submitMovesDraftToUnderReview() {
        Opportunity opportunity = draftOpportunity();
        Issuer issuer = opportunity.getIssuer();
        when(issuerRepository.findByUser_PublicId(any())).thenReturn(Optional.of(issuer));
        when(opportunityRepository.findByIdAndIssuer_Id(1L, 10L)).thenReturn(Optional.of(opportunity));
        when(opportunityRepository.save(any(Opportunity.class))).thenAnswer(invocation -> invocation.getArgument(0));

        var response = opportunityService.submitForReview(1L, issuerPrincipal());

        assertThat(response.status()).isEqualTo(OpportunityStatus.UNDER_REVIEW);
    }

    @Test
    void publishMovesApprovedToLiveAndSetsMaturityDate() {
        Opportunity opportunity = draftOpportunity();
        opportunity.setStatus(OpportunityStatus.APPROVED);
        when(opportunityRepository.findById(1L)).thenReturn(Optional.of(opportunity));
        when(opportunityRepository.save(any(Opportunity.class))).thenAnswer(invocation -> invocation.getArgument(0));

        var response = opportunityService.publish(1L);

        assertThat(response.status()).isEqualTo(OpportunityStatus.LIVE);
        assertThat(opportunity.getPublishedAt()).isNotNull();
        assertThat(opportunity.getMaturityDate()).isNotNull();
    }

    @Test
    void reviewRejectMovesBackToDraft() {
        Opportunity opportunity = draftOpportunity();
        opportunity.setStatus(OpportunityStatus.UNDER_REVIEW);
        when(opportunityRepository.findById(1L)).thenReturn(Optional.of(opportunity));
        when(opportunityRepository.save(any(Opportunity.class))).thenAnswer(invocation -> invocation.getArgument(0));

        var response = opportunityService.review(1L, new ReviewOpportunityRequest(ReviewAction.REJECT, "Incomplete docs"));

        assertThat(response.status()).isEqualTo(OpportunityStatus.DRAFT);
        assertThat(opportunity.getReviewComment()).isEqualTo("Incomplete docs");
    }

    @Test
    void adminTransitionToMaturedFromLive() {
        Opportunity opportunity = draftOpportunity();
        opportunity.setStatus(OpportunityStatus.LIVE);
        opportunity.setMaturityDate(java.time.LocalDate.now().plusDays(30));
        when(opportunityRepository.findById(1L)).thenReturn(Optional.of(opportunity));
        when(opportunityRepository.save(any(Opportunity.class))).thenAnswer(invocation -> invocation.getArgument(0));

        var response = opportunityService.adminTransition(
                1L,
                new AdminTransitionRequest(OpportunityStatus.MATURED)
        );

        assertThat(response.status()).isEqualTo(OpportunityStatus.MATURED);
        assertThat(opportunity.getMaturedAt()).isNotNull();
    }

    @Test
    void publishRejectsDraftOpportunity() {
        Opportunity opportunity = draftOpportunity();
        when(opportunityRepository.findById(1L)).thenReturn(Optional.of(opportunity));

        assertThatThrownBy(() -> opportunityService.publish(1L))
                .isInstanceOf(InvalidStateTransitionException.class);
    }

    @Test
    void updateRejectsNonDraftOpportunity() {
        Opportunity opportunity = draftOpportunity();
        opportunity.setStatus(OpportunityStatus.LIVE);
        when(opportunityRepository.findById(1L)).thenReturn(Optional.of(opportunity));

        assertThatThrownBy(() -> opportunityService.update(
                1L,
                new com.tranche.opportunity.dto.UpdateOpportunityRequest(
                        "Updated", null, null, null, null, null, null, null, null
                ),
                adminPrincipal()
        )).isInstanceOf(BusinessException.class);
    }

    private Opportunity draftOpportunity() {
        Issuer issuer = new Issuer();
        issuer.setId(10L);
        issuer.setCompanyName("Acme Corp");

        Opportunity opportunity = new Opportunity();
        opportunity.setId(1L);
        opportunity.setIssuer(issuer);
        opportunity.setTitle("Invoice #1");
        opportunity.setFaceValue(new BigDecimal("1000000"));
        opportunity.setDiscountRate(new BigDecimal("8.5"));
        opportunity.setTenureDays(90);
        opportunity.setMinimumLot(new BigDecimal("10000"));
        opportunity.setRiskGrade(RiskGrade.A);
        opportunity.setTotalUnits(100);
        opportunity.setRemainingUnits(100);
        opportunity.setUnitPrice(new BigDecimal("10000"));
        opportunity.setStatus(OpportunityStatus.DRAFT);
        return opportunity;
    }

    private UserPrincipal issuerPrincipal() {
        return new UserPrincipal(5L, UUID.randomUUID(), "issuer@tranche.local", "hash", Role.ISSUER, true);
    }

    private UserPrincipal adminPrincipal() {
        return new UserPrincipal(1L, UUID.randomUUID(), "admin@tranche.local", "hash", Role.ADMIN, true);
    }
}
