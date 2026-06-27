package com.crescendo.apps.mongodb;

import com.crescendo.app.App;
import com.crescendo.apps.AppDefinition;
import com.crescendo.enums.AuthType;
import org.springframework.stereotype.Component;

import java.util.List;
import java.util.Map;

/**
 * AppDefinition for MongoDB.
 */
@Component
public class MongoDbApp implements AppDefinition {

    @Override
    public App toApp() {
        return new App(
                "mongoDb",
                "MongoDB",
                """
                Find, insert and update documents in MongoDB.
                
                This integration provides operations for:
                - **Aggregate**: Aggregate documents
                - **Delete**: Delete documents
                - **Find**: Find documents
                - **Insert**: Insert documents
                - **Update**: Update documents
                - **Find One And Replace**: Find one document and replace it
                - **Find One And Update**: Find one document and update it
                - Search Index operations
                """,
                "https://www.google.com/s2/favicons?domain=mongodb.com&sz=128",
                AuthType.OAUTH2, // Placeholder, MongoDB uses connection string usually, but sticking to standard auth schemas
                List.of(),
                List.of(
                        Map.of(
                                "actionKey", "mongoDb:aggregate",
                                "name", "Aggregate",
                                "description", "Aggregate documents",
                                "configSchema", List.of(
                                        Map.of("key", "collection", "label", "Collection", "type", "text", "required", true),
                                        Map.of("key", "query", "label", "Query", "type", "json", "required", true)
                                )
                        ),
                        Map.of(
                                "actionKey", "mongoDb:delete",
                                "name", "Delete",
                                "description", "Delete documents",
                                "configSchema", List.of(
                                        Map.of("key", "collection", "label", "Collection", "type", "text", "required", true),
                                        Map.of("key", "query", "label", "Query", "type", "json", "required", true)
                                )
                        ),
                        Map.of(
                                "actionKey", "mongoDb:find",
                                "name", "Find",
                                "description", "Find documents",
                                "configSchema", List.of(
                                        Map.of("key", "collection", "label", "Collection", "type", "text", "required", true),
                                        Map.of("key", "query", "label", "Query", "type", "json", "required", true),
                                        Map.of("key", "options", "label", "Options", "type", "json")
                                )
                        ),
                        Map.of(
                                "actionKey", "mongoDb:insert",
                                "name", "Insert",
                                "description", "Insert documents",
                                "configSchema", List.of(
                                        Map.of("key", "collection", "label", "Collection", "type", "text", "required", true),
                                        Map.of("key", "fields", "label", "Fields", "type", "text"),
                                        Map.of("key", "options", "label", "Options", "type", "json")
                                )
                        ),
                        Map.of(
                                "actionKey", "mongoDb:update",
                                "name", "Update",
                                "description", "Update documents",
                                "configSchema", List.of(
                                        Map.of("key", "collection", "label", "Collection", "type", "text", "required", true),
                                        Map.of("key", "updateKey", "label", "Update Key", "type", "text", "required", true),
                                        Map.of("key", "fields", "label", "Fields", "type", "text"),
                                        Map.of("key", "upsert", "label", "Upsert", "type", "boolean"),
                                        Map.of("key", "options", "label", "Options", "type", "json")
                                )
                        )
                )
        ).credentialSchema(List.of(
                Map.of("key", "connectionString", "label", "Connection String", "type", "string", "required", true),
                Map.of("key", "database", "label", "Database", "type", "string", "required", true)
        )).category("databases");
    }
}
