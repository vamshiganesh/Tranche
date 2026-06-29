package com.tranche.support;

import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.test.context.ActiveProfiles;

/**
 * Integration tests against local MariaDB/Redis (docker compose).
 * Run {@code docker compose up -d} before executing these tests.
 */
@SpringBootTest
@ActiveProfiles("test")
public abstract class LocalDatabaseIntegrationTest {
}
