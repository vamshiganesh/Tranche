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
