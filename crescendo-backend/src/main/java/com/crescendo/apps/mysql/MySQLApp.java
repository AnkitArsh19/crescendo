package com.crescendo.apps.mysql;

import com.crescendo.app.App;
import com.crescendo.apps.AppDefinition;
import com.crescendo.enums.AuthType;
import org.springframework.stereotype.Component;

import java.util.List;
import java.util.Map;

@Component
public class MySqlApp implements AppDefinition {

    @Override
    public App toApp() {
        return new App("mysql", "MySQL", """
                MySQL is an open-source relational database management system. This integration allows you to run SQL queries securely from your Crescendo workflows.
                
                **What you can do with MySQL in Crescendo:**
                - Fetch user data to enrich incoming support tickets
                - Insert analytics events into a data warehouse automatically
                - Run regular maintenance queries or trigger workflows based on database state
                
                **Actions available:**
                - Execute Query — run any valid SQL query (SELECT, INSERT, UPDATE, DELETE) against the configured database
                
                **Who should use this:** Database administrators, backend developers, and data engineers automating database tasks.
                
                **Authentication:** Requires host, port, database name, username, and password credentials.
                """,
                "https://www.google.com/s2/favicons?domain=mysql.com&sz=128", AuthType.NONE,
                List.of(),
                List.of(
                        Map.of("actionKey", "mySql:executeQuery", "name", "Execute Query", "description", "Execute a SQL query",
                                "configSchema", List.of(
                                        Map.of("key", "query", "label", "SQL Query", "type", "textarea", "required", true)
                                ))
                )
        ).credentialSchema(List.of(
                Map.of("key", "host", "label", "Host", "type", "text", "required", true),
                Map.of("key", "port", "label", "Port", "type", "text", "required", false, "placeholder", "3306"),
                Map.of("key", "database", "label", "Database", "type", "text", "required", true),
                Map.of("key", "username", "label", "Username", "type", "text", "required", true),
                Map.of("key", "password", "label", "Password", "type", "password", "required", true)
        )).category("database");
    }
}
