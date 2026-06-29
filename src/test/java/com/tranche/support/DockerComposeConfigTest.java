package com.tranche.support;

import org.junit.jupiter.api.Test;

import java.nio.file.Files;
import java.nio.file.Path;

import static org.assertj.core.api.Assertions.assertThat;

class DockerComposeConfigTest {

    @Test
    void composeDefinesMariaDbAndRedisWithHealthchecks() throws Exception {
        Path compose = Path.of("docker-compose.yml");
        assertThat(compose).exists();

        String yaml = Files.readString(compose);
        assertThat(yaml).contains("mariadb:11.4");
        assertThat(yaml).contains("redis:7-alpine");
        assertThat(yaml).contains("healthcheck:");
        assertThat(yaml).contains("docker/mariadb/init");
    }

    @Test
    void dockerfileBuildsSpringBootJar() throws Exception {
        Path dockerfile = Path.of("Dockerfile");
        assertThat(dockerfile).exists();
        String content = Files.readString(dockerfile);
        assertThat(content).contains("eclipse-temurin:21");
        assertThat(content).contains("app.jar");
    }

    @Test
    void mariadbInitCreatesTestDatabase() throws Exception {
        Path init = Path.of("docker/mariadb/init/01-create-test-db.sql");
        assertThat(init).exists();
        String sql = Files.readString(init);
        assertThat(sql).containsIgnoringCase("tranche_test");
    }
}
