package com.crescendo.apps.mongodb;

import com.crescendo.app.App;
import com.crescendo.apps.AppDefinition;
import com.crescendo.enums.AuthType;
import org.springframework.stereotype.Component;
import java.util.List;
import java.util.Map;

@Component
public class MongoDbApp implements AppDefinition {
    @Override public App toApp() {
        return new App("mongodb", "MongoDB", "Use MongoDB with Atlas Data API or a direct MongoDB connection URI",
                "/icons/mongodb.svg", AuthType.APIKEY, List.of(), List.of(
                Map.of("actionKey", "find", "name", "Find Documents", "description", "Find documents in a collection",
                        "configSchema", List.of(
                                Map.of("key", "database", "label", "Database", "type", "text", "required", true),
                                Map.of("key", "collection", "label", "Collection", "type", "text", "required", true),
                                Map.of("key", "filter", "label", "Filter (JSON)", "type", "json", "required", false),
                                Map.of("key", "limit", "label", "Limit", "type", "text", "required", false, "placeholder", "20"))),
                Map.of("actionKey", "insert-one", "name", "Insert One", "description", "Insert one document",
                        "configSchema", List.of(
                                Map.of("key", "database", "label", "Database", "type", "text", "required", true),
                                Map.of("key", "collection", "label", "Collection", "type", "text", "required", true),
                                Map.of("key", "document", "label", "Document (JSON)", "type", "json", "required", true))))
        ).credentialSchema(List.of(
                Map.of("key", "connectionUri", "label", "Connection URI", "type", "password", "required", false,
                        "placeholder", "mongodb+srv://user:password@cluster.example.net/?retryWrites=true&w=majority",
                        "helpText", "Use for direct MongoDB/Atlas cluster connections"),
                Map.of("key", "endpoint", "label", "Data API Endpoint", "type", "text", "required", false,
                        "helpText", "Use for Atlas Data API mode"),
                Map.of("key", "apiKey", "label", "Data API Key", "type", "password", "required", false),
                Map.of("key", "dataSource", "label", "Data Source", "type", "text", "required", false, "placeholder", "Cluster0")
        )).category("database").helpUrl("https://www.mongodb.com/docs/drivers/java/sync/current/");
    }
}
