package com.crescendo.execution.engine;

import com.crescendo.apps.postgresql.PostgreSQLExecuteQueryHandler;
import com.crescendo.execution.action.ActionContext;
import com.crescendo.execution.action.ActionResult;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import tools.jackson.databind.ObjectMapper;

import java.util.Map;
import java.util.UUID;

import static org.junit.jupiter.api.Assertions.*;

class PostgreSqlHandlersTest {

    private final PostgreSQLExecuteQueryHandler handler = new PostgreSQLExecuteQueryHandler(new ObjectMapper());

    private ActionContext createContext(Map<String, Object> config, Map<String, Object> credentials) {
        return new ActionContext("postgresql", "execute-query", config, credentials, Map.of(), UUID.randomUUID(), UUID.randomUUID(), UUID.randomUUID(), 1);
    }

    @Test
    @DisplayName("PostgreSQL Handler rejects execution when SQL is missing")
    void execute_failsWhenSqlMissing() {
        ActionContext context = createContext(Map.of(), Map.of());
        ActionResult result = handler.execute(context);

        assertFalse(result.success());
        assertTrue(result.error().contains("PostgreSQL SQL is required"));
    }

    @Test
    @DisplayName("PostgreSQL Handler fails gracefully when connection cannot be established")
    void execute_failsGracefullyOnUnreachableHost() {
        Map<String, Object> config = Map.of("sql", "SELECT 1");
        Map<String, Object> credentials = Map.of(
                "host", "127.0.0.1",
                "port", "54321", // Invalid port
                "database", "testdb",
                "username", "postgres",
                "password", "secret"
        );

        ActionContext context = createContext(config, credentials);
        ActionResult result = handler.execute(context);

        assertFalse(result.success());
        assertTrue(result.error().contains("PostgreSQL query failed"));
    }
}
