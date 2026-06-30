package com.tranche.support;

import org.testcontainers.containers.GenericContainer;
import org.testcontainers.containers.MariaDBContainer;
import org.testcontainers.utility.DockerImageName;

/**
 * Shared Testcontainers for the full integration-test suite.
 * Started once per JVM so containers are not torn down between test classes.
 */
public final class IntegrationTestContainers {

    private static final MariaDBContainer<?> MARIA_DB = new MariaDBContainer<>(DockerImageName.parse("mariadb:11.4"))
            .withDatabaseName("tranche_test")
            .withUsername("tranche")
            .withPassword("tranche");

    private static final GenericContainer<?> REDIS = new GenericContainer<>(DockerImageName.parse("redis:7-alpine"))
            .withExposedPorts(6379);

    static {
        MARIA_DB.start();
        REDIS.start();
    }

    private IntegrationTestContainers() {
    }

    public static MariaDBContainer<?> mariaDb() {
        return MARIA_DB;
    }

    public static GenericContainer<?> redis() {
        return REDIS;
    }
}
