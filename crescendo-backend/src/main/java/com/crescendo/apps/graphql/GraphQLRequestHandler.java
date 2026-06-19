package com.crescendo.apps.graphql;

import com.crescendo.execution.action.ActionContext;
import com.crescendo.execution.action.ActionHandler;
import com.crescendo.execution.action.ActionMapping;
import com.crescendo.execution.action.ActionResult;
import org.springframework.http.HttpHeaders;
import org.springframework.http.MediaType;
import org.springframework.stereotype.Component;
import org.springframework.web.client.RestClient;

import java.util.HashMap;
import java.util.Map;

@Component
@ActionMapping(appKey = "graphql", actionKey = "request")
public class GraphQLRequestHandler implements ActionHandler {

    private final RestClient restClient = RestClient.builder()
            .defaultHeader(HttpHeaders.ACCEPT, MediaType.APPLICATION_JSON_VALUE)
            .build();

    @Override
public ActionResult execute(ActionContext context) {
        Map<String, Object> config = context.configuration();
        if (config.get("url") == null || config.get("query") == null) {
            return ActionResult.failure("GraphQL request requires url and query");
        }

        Map<String, Object> body = new HashMap<>();
        body.put("query", config.get("query"));
        if (config.get("variables") != null) {
            body.put("variables", config.get("variables"));
        }

        try {
            RestClient.RequestBodySpec spec = restClient.post()
                    .uri(String.valueOf(config.get("url")))
                    .contentType(MediaType.APPLICATION_JSON);
            applyHeaders(spec, config.get("headers"));
            Object token = context.credentials().get("accessToken");
            if (token != null && !String.valueOf(token).isBlank()) {
                spec.header(HttpHeaders.AUTHORIZATION, "Bearer " + token);
            }
            String response = spec.body(body).retrieve().body(String.class);
            return ActionResult.success(Map.of("body", response));
        } catch (Exception e) {
            return ActionResult.failure("GraphQL request failed: " + e.getMessage());
        }
    }

    private void applyHeaders(RestClient.RequestBodySpec spec, Object headersObj) {
        if (headersObj instanceof Map<?, ?> headers) {
            headers.forEach((key, value) -> spec.header(String.valueOf(key), value == null ? "" : String.valueOf(value)));
        }
    }
}
