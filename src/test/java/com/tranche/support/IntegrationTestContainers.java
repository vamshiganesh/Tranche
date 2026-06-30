package com.tranche.support;

import org.testcontainers.containers.GenericContainer;
import org.testcontainers.containers.MariaDBContainer;
import org.testcontainers.utility.DockerImageName;

/**
 * Shared Testcontainers for the full integration-test suite.
 * Started once per JVM so containers are not torn down between test classes.
 */
public final class IntegrationTestContainers {

    private static MariaDBContainer<?> mariaDb;
    private static GenericContainer<?> redis;

    private IntegrationTestContainers() {
    }

    public static MariaDBContainer<?> mariaDb() {
        if (mariaDb == null) {
            mariaDb = new MariaDBContainer<>(DockerImageName.parse("mariadb:11.4"))
                    .withDatabaseName("tranche_test")
                    .withUsername("tranche")
                    .withPassword("tranche");
            mariaDb.start();
        }
        return mariaDb;
    }

    public static GenericContainer<?> redis() {
        if (redis == null) {
            redis = new GenericContainer<>(DockerImageName.parse("redis:7-alpine"))
                    .withExposedPorts(6379);
            redis.start();
        }
        return redis;
    }
}
