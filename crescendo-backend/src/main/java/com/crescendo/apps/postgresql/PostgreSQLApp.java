package com.crescendo.apps.postgresql;

import com.crescendo.app.App;
import com.crescendo.apps.AppDefinition;
import com.crescendo.enums.AuthType;
import org.springframework.stereotype.Component;

import java.util.List;
import java.util.Map;

@Component
public class PostgreSQLApp implements AppDefinition {

    @Override
    public App toApp() {
        return new App("postgresql", "PostgreSQL", "Run SQL queries against a user-owned PostgreSQL database",
                "/icons/postgresql.svg", AuthType.APIKEY,
                List.of(),
                List.of(
                        Map.of("actionKey", "execute-query", "name", "Execute Query",
                                "description", "Run a SELECT/INSERT/UPDATE/DELETE statement",
                                "configSchema", List.of(
                                        Map.of("key", "sql", "label", "SQL", "type", "textarea", "required", true,
                                                "placeholder", "select * from users where id = ?"),
                                        Map.of("key", "params", "label", "Parameters (JSON Array)", "type", "json", "required", false,
                                                "placeholder", "[123]"),
                                        Map.of("key", "maxRows", "label", "Max Rows", "type", "text", "required", false,
                                                "placeholder", "100")))
                )
        ).credentialSchema(List.of(
                Map.of("key", "host", "label", "Host", "type", "text", "required", true,
                        "placeholder", "localhost"),
                Map.of("key", "port", "label", "Port", "type", "text", "required", false,
                        "placeholder", "5432"),
                Map.of("key", "database", "label", "Database", "type", "text", "required", true),
                Map.of("key", "username", "label", "Username", "type", "text", "required", true),
                Map.of("key", "password", "label", "Password", "type", "password", "required", true),
                Map.of("key", "sslMode", "label", "SSL Mode", "type", "text", "required", false,
                        "placeholder", "prefer")
        )).category("database").helpUrl("https://www.postgresql.org/docs/current/");
    }
}
