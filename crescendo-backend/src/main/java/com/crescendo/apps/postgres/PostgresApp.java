package com.crescendo.apps.postgres;

import com.crescendo.app.App;
import com.crescendo.apps.AppDefinition;
import com.crescendo.enums.AuthType;
import org.springframework.stereotype.Component;

import java.util.List;
import java.util.Map;

/**
 * AppDefinition for Postgres.
 */
@Component
public class PostgresApp implements AppDefinition {

    @Override
    public App toApp() {
        return new App(
                "postgres",
                "Postgres",
                """
                Get, add and update data in Postgres.
                
                This integration provides operations for:
                - **Delete**: Delete an entire table or rows in a table
                - **Execute Query**: Execute an SQL query
                - **Insert**: Insert rows in a table
                - **Insert or Update**: Insert or update rows in a table
                - **Select**: Select rows from a table
                - **Update**: Update rows in a table
                """,
                "https://www.google.com/s2/favicons?domain=postgresql.org&sz=128", // Generic icon
                AuthType.OAUTH2, // Placeholder
                List.of(),
                List.of(
                        Map.of(
                                "actionKey", "postgres:deleteTable",
                                "name", "Delete",
                                "description", "Delete an entire table or rows in a table",
                                "configSchema", List.of(
                                        Map.of("key", "schema", "label", "Schema", "type", "text", "default", "public"),
                                        Map.of("key", "table", "label", "Table", "type", "text", "required", true),
                                        Map.of("key", "deleteBy", "label", "Delete By", "type", "text", "default", "id"),
                                        Map.of("key", "id", "label", "ID", "type", "text")
                                )
                        ),
                        Map.of(
                                "actionKey", "postgres:executeQuery",
                                "name", "Execute Query",
                                "description", "Execute an SQL query",
                                "configSchema", List.of(
                                        Map.of("key", "query", "label", "Query", "type", "text", "required", true)
                                )
                        ),
                        Map.of(
                                "actionKey", "postgres:insert",
                                "name", "Insert",
                                "description", "Insert rows in a table",
                                "configSchema", List.of(
                                        Map.of("key", "schema", "label", "Schema", "type", "text", "default", "public"),
                                        Map.of("key", "table", "label", "Table", "type", "text", "required", true),
                                        Map.of("key", "columns", "label", "Columns", "type", "text")
                                )
                        ),
                        Map.of(
                                "actionKey", "postgres:upsert",
                                "name", "Insert or Update",
                                "description", "Insert or update rows in a table",
                                "configSchema", List.of(
                                        Map.of("key", "schema", "label", "Schema", "type", "text", "default", "public"),
                                        Map.of("key", "table", "label", "Table", "type", "text", "required", true),
                                        Map.of("key", "columns", "label", "Columns", "type", "text")
                                )
                        ),
                        Map.of(
                                "actionKey", "postgres:select",
                                "name", "Select",
                                "description", "Select rows from a table",
                                "configSchema", List.of(
                                        Map.of("key", "schema", "label", "Schema", "type", "text", "default", "public"),
                                        Map.of("key", "table", "label", "Table", "type", "text", "required", true),
                                        Map.of("key", "selectBy", "label", "Select By", "type", "text", "default", "all")
                                )
                        ),
                        Map.of(
                                "actionKey", "postgres:update",
                                "name", "Update",
                                "description", "Update rows in a table",
                                "configSchema", List.of(
                                        Map.of("key", "schema", "label", "Schema", "type", "text", "default", "public"),
                                        Map.of("key", "table", "label", "Table", "type", "text", "required", true),
                                        Map.of("key", "updateBy", "label", "Update By", "type", "text", "default", "id"),
                                        Map.of("key", "id", "label", "ID", "type", "text"),
                                        Map.of("key", "columns", "label", "Columns", "type", "text")
                                )
                        )
                )
        ).credentialSchema(List.of(
                Map.of("key", "host", "label", "Host", "type", "string", "required", true),
                Map.of("key", "port", "label", "Port", "type", "number", "required", true, "default", 5432),
                Map.of("key", "database", "label", "Database", "type", "string", "required", true),
                Map.of("key", "user", "label", "User", "type", "string", "required", true),
                Map.of("key", "password", "label", "Password", "type", "string")
        )).category("databases");
    }
}
