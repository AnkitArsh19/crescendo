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
            .withPassword("testpass");

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
        
        registry.add("spring.data.redis.host", redis::getHost);
        registry.add("spring.data.redis.port", () -> redis.getMappedPort(6379));
    }
}
