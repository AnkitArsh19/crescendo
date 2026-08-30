package com.crescendo;

import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.test.context.DynamicPropertyRegistry;
import org.springframework.test.context.DynamicPropertySource;
import org.testcontainers.containers.GenericContainer;
import org.testcontainers.containers.PostgreSQLContainer;

@SpringBootTest
public abstract class BaseIntegrationTest {

    @SuppressWarnings("resource")
    public static final PostgreSQLContainer<?> postgres = new PostgreSQLContainer<>("postgres:15-alpine")
            .withDatabaseName("crescendo_test")
            .withUsername("testuser")
            .withPassword("testpass")
            .withCommand("postgres", "-c", "max_connections=300");

    @SuppressWarnings("resource")
    public static final GenericContainer<?> redis = new GenericContainer<>("redis:7-alpine")
            .withExposedPorts(6379);

    static {
        postgres.start();
        redis.start();
    }

    @DynamicPropertySource
    static void configureProperties(DynamicPropertyRegistry registry) {
        registry.add("spring.datasource.command.url", postgres::getJdbcUrl);
        registry.add("spring.datasource.command.username", postgres::getUsername);
        registry.add("spring.datasource.command.password", postgres::getPassword);
        registry.add("spring.datasource.query.url", postgres::getJdbcUrl);
        registry.add("spring.datasource.query.username", postgres::getUsername);
        registry.add("spring.datasource.query.password", postgres::getPassword);

        // Size Hikari connection pools conservatively for test suites to prevent client exhaustion
        registry.add("spring.datasource.command.hikari.maximum-pool-size", () -> 5);
        registry.add("spring.datasource.command.hikari.minimum-idle", () -> 1);
        registry.add("spring.datasource.query.hikari.maximum-pool-size", () -> 5);
        registry.add("spring.datasource.query.hikari.minimum-idle", () -> 1);

        registry.add("spring.data.redis.host", redis::getHost);
        registry.add("spring.data.redis.port", () -> redis.getMappedPort(6379));
        registry.add("spring.data.redis.repositories.enabled", () -> "false");

        // Rate limiter can be disabled during automated integration tests
        registry.add("app.security.rate-limit.auth.enabled", () -> "false");
    }
}
