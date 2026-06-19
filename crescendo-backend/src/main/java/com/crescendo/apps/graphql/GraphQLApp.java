package com.crescendo.apps.graphql;

import com.crescendo.app.App;
import com.crescendo.apps.AppDefinition;
import com.crescendo.enums.AuthType;
import org.springframework.stereotype.Component;

import java.util.List;
import java.util.Map;

@Component
public class GraphQLApp implements AppDefinition {
    @Override
    public App toApp() {
        return new App("graphql", "GraphQL", "Call GraphQL APIs with query and variables",
                "/icons/graphql.svg", AuthType.NONE,
                List.of(),
                List.of(
                        Map.of("actionKey", "request", "name", "GraphQL Request",
                                "description", "Send a GraphQL query or mutation",
                                "configSchema", List.of(
                                        Map.of("key", "url", "label", "Endpoint URL", "type", "text", "required", true,
                                                "placeholder", "https://api.example.com/graphql", "helpText", "GraphQL endpoint"),
                                        Map.of("key", "query", "label", "Query", "type", "textarea", "required", true,
                                                "placeholder", "query { viewer { id } }", "helpText", "GraphQL query or mutation"),
                                        Map.of("key", "variables", "label", "Variables", "type", "json", "required", false,
                                                "placeholder", "{\"id\":\"123\"}", "helpText", "GraphQL variables object"),
                                        Map.of("key", "headers", "label", "Headers", "type", "json", "required", false,
                                                "placeholder", "{\"Authorization\":\"Bearer ...\"}", "helpText", "Optional request headers")
                                ))
                )
        ).credentialSchema(List.of(
                Map.of("key", "accessToken", "label", "Access Token", "type", "password", "required", false,
                        "helpText", "Optional bearer token for APIs that require authentication")
        )).category("developer").helpUrl("");
    }
}
