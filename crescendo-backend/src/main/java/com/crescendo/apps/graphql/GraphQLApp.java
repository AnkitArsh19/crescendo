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
        return new App(
                "graphql",
                "GraphQL", 
                """
                GraphQL is a query language for your API, and a server-side runtime for executing queries. This integration lets you execute any GraphQL query or mutation against a remote endpoint.
                
                **What you can do with GraphQL in Crescendo:**
                - Fetch specific user or product details from a unified graph API
                - Execute complex data mutations with precise payload requirements
                - Automate data synchronization across different GraphQL services
                
                **Actions available:**
                - Execute — run a GraphQL query or mutation, passing in variables securely
                
                **Who should use this:** Backend developers, frontend engineers, and data engineers integrating with GraphQL APIs.
                
                **Authentication:** Supports Bearer tokens or Basic Authentication via connection credentials.
                """,
                "https://www.google.com/s2/favicons?domain=graphql.org&sz=128", 
                AuthType.APIKEY,
                List.of(),
                List.of(
                        Map.of(
                                "actionKey", "execute",
                                "name", "Execute",
                                "description", "Execute a GraphQL query or mutation",
                                "configSchema", List.of(
                                        Map.of("key", "url", "label", "Endpoint URL", "type", "text", "required", true),
                                        Map.of("key", "query", "label", "Query", "type", "textarea", "required", true),
                                        Map.of("key", "variables", "label", "Variables (JSON)", "type", "textarea", "required", false)
                                )
                        )
                )
        ).credentialSchema(List.of(
                Map.of("key", "authHeader", "label", "Authorization Header", "type", "text", "required", false, "placeholder", "Bearer token")
        )).category("developer");
    }
}
