package com.tranche.support;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.tranche.allocation.repository.AllocationRepository;
import com.tranche.allocation.repository.InvestmentOrderRepository;
import com.tranche.audit.repository.AuditLogRepository;
import com.tranche.auth.domain.User;
import com.tranche.auth.repository.UserRepository;
import com.tranche.common.domain.Role;
import com.tranche.common.domain.VerificationStatus;
import com.tranche.common.security.UserPrincipal;
import com.tranche.investor.repository.InvestorProfileRepository;
import com.tranche.issuer.domain.Issuer;
import com.tranche.issuer.repository.IssuerRepository;
import com.tranche.notification.repository.OutboxEventRepository;
import com.tranche.opportunity.domain.Opportunity;
import com.tranche.opportunity.repository.OpportunityRepository;
import com.tranche.portfolio.repository.PortfolioPositionRepository;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.test.context.DynamicPropertyRegistry;
import org.springframework.test.context.DynamicPropertySource;
import org.springframework.transaction.PlatformTransactionManager;
import org.springframework.transaction.support.TransactionTemplate;
import org.testcontainers.junit.jupiter.Testcontainers;

import java.math.BigDecimal;
import java.util.List;

@SpringBootTest
@Testcontainers(disabledWithoutDocker = true)
@ActiveProfiles("test")
public abstract class AbstractIntegrationTest {

    @DynamicPropertySource
    static void configureProperties(DynamicPropertyRegistry registry) {
        registry.add("spring.datasource.url", IntegrationTestContainers.mariaDb()::getJdbcUrl);
        registry.add("spring.datasource.username", IntegrationTestContainers.mariaDb()::getUsername);
        registry.add("spring.datasource.password", IntegrationTestContainers.mariaDb()::getPassword);
        registry.add("spring.data.redis.host", IntegrationTestContainers.redis()::getHost);
        registry.add("spring.data.redis.port", () -> IntegrationTestContainers.redis().getMappedPort(6379));
        registry.add("spring.cache.type", () -> "none");
    }

    @Autowired
    protected IssuerRepository issuerRepository;

    @Autowired
    protected UserRepository userRepository;

    @Autowired
    protected InvestorProfileRepository investorProfileRepository;

    @Autowired
    protected OpportunityRepository opportunityRepository;

    @Autowired
    protected InvestmentOrderRepository investmentOrderRepository;

    @Autowired
    protected AllocationRepository allocationRepository;

    @Autowired
    protected PortfolioPositionRepository portfolioPositionRepository;

    @Autowired
    protected AuditLogRepository auditLogRepository;

    @Autowired
    protected OutboxEventRepository outboxEventRepository;

    @Autowired
    protected ObjectMapper objectMapper;

    @Autowired
    private PlatformTransactionManager transactionManager;

    protected void resetInvestorWallets() {
        new TransactionTemplate(transactionManager).executeWithoutResult(status ->
                investorProfileRepository.resetAllWalletsForTests(
                        new BigDecimal("3000000.0000"),
                        VerificationStatus.APPROVED
                )
        );
    }

    protected void clearTransactionalData() {
        portfolioPositionRepository.deleteAll();
        allocationRepository.deleteAll();
        investmentOrderRepository.deleteAll();
        auditLogRepository.deleteAll();
        outboxEventRepository.deleteAll();
        opportunityRepository.findAll().stream()
                .filter(opportunity -> !SeedUsers.DEMO_OPPORTUNITY_TITLE.equals(opportunity.getTitle()))
                .forEach(opportunityRepository::delete);
    }

    protected User requireUser(String email) {
        return userRepository.findByEmail(email)
                .orElseThrow(() -> new IllegalStateException("User not found: " + email));
    }

    protected UserPrincipal principal(String email) {
        return UserPrincipal.from(requireUser(email));
    }

    protected List<UserPrincipal> investorPrincipals() {
        return userRepository.findAll().stream()
                .filter(user -> user.getRole() == Role.INVESTOR)
                .filter(user -> investorProfileRepository.findByUser_Id(user.getId())
                        .map(profile -> profile.getKycStatus() == VerificationStatus.APPROVED)
                        .orElse(false))
                .map(UserPrincipal::from)
                .toList();
    }

    protected List<UserPrincipal> seededInvestorPrincipals() {
        return List.of(
                principal(SeedUsers.INVESTOR1_EMAIL),
                principal(SeedUsers.INVESTOR2_EMAIL)
        );
    }

    /** Creates a LIVE opportunity for allocation integration tests. */
    protected Long prepareLiveOpportunity(String title, int totalUnits, int remainingUnits) {
        resetInvestorWallets();
        clearTransactionalData();
        Issuer issuer = issuerRepository.findAll().getFirst();
        Opportunity opportunity = OpportunityTestBuilder.anOpportunity()
                .issuer(issuer)
                .title(title)
                .totalUnits(totalUnits)
                .remainingUnits(remainingUnits)
                .live()
                .build();
        return opportunityRepository.save(opportunity).getId();
    }
}
