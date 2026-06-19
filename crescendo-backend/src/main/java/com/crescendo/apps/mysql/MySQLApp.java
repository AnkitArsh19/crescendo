package com.crescendo.apps.mysql;

import com.crescendo.app.App;
import com.crescendo.apps.AppDefinition;
import com.crescendo.enums.AuthType;
import org.springframework.stereotype.Component;
import java.util.List;
import java.util.Map;

@Component
public class MySQLApp implements AppDefinition {
    @Override
    public App toApp() {
        return new App("mysql", "MySQL", "Run SQL queries against a user-owned MySQL or MariaDB database",
                "/icons/mysql.svg", AuthType.APIKEY, List.of(), List.of(
                Map.of("actionKey", "execute-query", "name", "Execute Query",
                        "description", "Run a SELECT/INSERT/UPDATE/DELETE statement",
                        "configSchema", List.of(
                                Map.of("key", "sql", "label", "SQL", "type", "textarea", "required", true),
                                Map.of("key", "params", "label", "Parameters (JSON Array)", "type", "json", "required", false),
                                Map.of("key", "maxRows", "label", "Max Rows", "type", "text", "required", false, "placeholder", "100"))))
        ).credentialSchema(List.of(
                Map.of("key", "host", "label", "Host", "type", "text", "required", true),
                Map.of("key", "port", "label", "Port", "type", "text", "required", false, "placeholder", "3306"),
                Map.of("key", "database", "label", "Database", "type", "text", "required", true),
                Map.of("key", "username", "label", "Username", "type", "text", "required", true),
                Map.of("key", "password", "label", "Password", "type", "password", "required", true)
        )).category("database").helpUrl("https://dev.mysql.com/doc/");
    }
}
