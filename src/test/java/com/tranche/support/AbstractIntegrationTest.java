package com.tranche.support;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.tranche.allocation.repository.AllocationRepository;
import com.tranche.allocation.repository.InvestmentOrderRepository;
import com.tranche.audit.repository.AuditLogRepository;
import com.tranche.auth.domain.User;
import com.tranche.auth.repository.UserRepository;
import com.tranche.common.domain.Role;
import com.tranche.common.security.UserPrincipal;
import com.tranche.investor.domain.InvestorProfile;
import com.tranche.investor.repository.InvestorProfileRepository;
import com.tranche.notification.repository.OutboxEventRepository;
import com.tranche.opportunity.repository.OpportunityRepository;
import com.tranche.portfolio.repository.PortfolioPositionRepository;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.test.context.DynamicPropertyRegistry;
import org.springframework.test.context.DynamicPropertySource;
import org.testcontainers.containers.GenericContainer;
import org.testcontainers.containers.MariaDBContainer;
import org.testcontainers.junit.jupiter.Container;
import org.testcontainers.junit.jupiter.Testcontainers;
import org.testcontainers.utility.DockerImageName;

import java.math.BigDecimal;
import java.util.List;

@SpringBootTest
@Testcontainers(disabledWithoutDocker = true)
@ActiveProfiles("test")
public abstract class AbstractIntegrationTest {

    @Container
    static MariaDBContainer<?> mariaDB = new MariaDBContainer<>(DockerImageName.parse("mariadb:11.4"))
            .withDatabaseName("tranche_test")
            .withUsername("tranche")
            .withPassword("tranche");

    @Container
    static GenericContainer<?> redis = new GenericContainer<>(DockerImageName.parse("redis:7-alpine"))
            .withExposedPorts(6379);

    @DynamicPropertySource
    static void configureProperties(DynamicPropertyRegistry registry) {
        registry.add("spring.datasource.url", mariaDB::getJdbcUrl);
        registry.add("spring.datasource.username", mariaDB::getUsername);
        registry.add("spring.datasource.password", mariaDB::getPassword);
        registry.add("spring.data.redis.host", redis::getHost);
        registry.add("spring.data.redis.port", () -> redis.getMappedPort(6379));
        registry.add("spring.cache.type", () -> "none");
    }

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

    protected void resetInvestorWallets() {
        for (InvestorProfile profile : investorProfileRepository.findAll()) {
            profile.setWalletBalance(new BigDecimal("500000.0000"));
            profile.setLockedBalance(BigDecimal.ZERO);
            investorProfileRepository.save(profile);
        }
    }

    protected void clearTransactionalData() {
        portfolioPositionRepository.deleteAll();
        allocationRepository.deleteAll();
        investmentOrderRepository.deleteAll();
        auditLogRepository.deleteAll();
        outboxEventRepository.deleteAll();
        opportunityRepository.deleteAll();
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
                .map(UserPrincipal::from)
                .toList();
    }
}
