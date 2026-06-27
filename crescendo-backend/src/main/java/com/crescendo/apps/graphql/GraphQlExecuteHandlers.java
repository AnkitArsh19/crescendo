package com.crescendo.apps.graphql;

import com.crescendo.execution.action.ActionContext;
import com.crescendo.execution.action.ActionMapping;
import com.crescendo.utils.RestClient;
import org.springframework.stereotype.Component;

import java.util.HashMap;
import java.util.Map;

/**
 * GraphQL handlers.
 */
@Component
public class GraphQlExecuteHandlers {

    @ActionMapping(appKey = "graphql", actionKey = "execute")
    public Object executeRequest(ActionContext context) throws Exception {
        String endpoint = context.getString("endpoint");
        String requestMethod = context.getString("requestMethod"); // Default POST
        if (requestMethod == null) requestMethod = "POST";
        String requestFormat = context.getString("requestFormat"); // Default json
        String query = context.getString("query");
        Map<String, Object> variables = context.getMap("variables");
        String operationName = context.getString("operationName");
        Map<String, Object> headerParameters = context.getMap("headerParameters");
        
        RestClient.Builder client = RestClient.builder().url(endpoint);
        
        // Add headers
        if (headerParameters != null) {
            headerParameters.forEach((k, v) -> client.header(k, v.toString()));
        }

        // Add Authentication
        String authType = context.getCredential("authType");
        if ("basicAuth".equals(authType)) {
            String user = context.getCredential("user");
            String password = context.getCredential("password");
            client.basicAuth(user, password);
        } else if ("headerAuth".equals(authType)) {
            String headerName = context.getCredential("headerName");
            String headerValue = context.getCredential("headerValue");
            client.header(headerName, headerValue);
        }

        if ("POST".equalsIgnoreCase(requestMethod)) {
            if ("json".equals(requestFormat)) {
                client.header("Content-Type", "application/json");
                Map<String, Object> body = new HashMap<>();
                body.put("query", query);
                if (variables != null) body.put("variables", variables);
                if (operationName != null) body.put("operationName", operationName);
                client.post(body);
            } else {
                client.header("Content-Type", "application/graphql");
                client.post(query);
            }
        } else {
            // GET
            client.queryParam("query", query);
            client.get();
        }

        return client.execute();
    }
}
