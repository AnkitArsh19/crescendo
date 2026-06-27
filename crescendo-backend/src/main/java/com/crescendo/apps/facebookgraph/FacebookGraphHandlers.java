package com.crescendo.apps.facebookgraph;

import com.crescendo.execution.action.ActionContext;
import com.crescendo.execution.action.ActionMapping;
import com.crescendo.execution.action.ActionResult;
import org.springframework.stereotype.Component;
import org.springframework.web.client.RestClient;

import java.util.Map;
import java.util.HashMap;

@Component
public class FacebookGraphHandlers {

    private static final String DEFAULT_VERSION = "v20.0";

    private String getVersion(ActionContext context) {
        String ver = (String) context.credentials().get("graphVersion");
        return (ver != null && !ver.isBlank()) ? ver : DEFAULT_VERSION;
    }

    private String getAuth(ActionContext context) {
        return "Bearer " + context.credentials().get("accessToken");
    }

    @ActionMapping(appKey = "facebook-graph", actionKey = "create-page-post")
    public Object createPost(ActionContext context) throws Exception {
        String pageId = context.configuration().get("pageId") != null ? context.configuration().get("pageId").toString() : "";
        String message = context.configuration().get("message") != null ? context.configuration().get("message").toString() : "";

        if (pageId.isBlank() || message.isBlank()) {
            return ActionResult.failure("Page ID and message are required");
        }

        Map<String, String> body = new HashMap<>();
        body.put("message", message);

        try {
            String response = RestClient.create("https://graph.facebook.com")
                    .post()
                    .uri("/{version}/{pageId}/feed", getVersion(context), pageId)
                    .header("Authorization", getAuth(context))
                    .header("Content-Type", "application/json")
                    .body(body)
                    .retrieve()
                    .body(String.class);
            return ActionResult.success(Map.of("data", response));
        } catch (Exception e) {
            return ActionResult.failure("Failed to create Facebook Page post: " + e.getMessage());
        }
    }

    @ActionMapping(appKey = "facebook-graph", actionKey = "get-node")
    public Object getNode(ActionContext context) throws Exception {
        String nodeId = context.configuration().get("nodeId") != null ? context.configuration().get("nodeId").toString() : "";
        String fields = context.configuration().get("fields") != null ? context.configuration().get("fields").toString() : "";

        if (nodeId.isBlank()) {
            return ActionResult.failure("Node ID is required");
        }

        try {
            RestClient.RequestHeadersSpec<?> spec = RestClient.create("https://graph.facebook.com")
                    .get()
                    .uri(uriBuilder -> uriBuilder
                        .path("/{version}/{nodeId}")
                        .queryParamIfPresent("fields", java.util.Optional.ofNullable(fields.isBlank() ? null : fields))
                        .build(getVersion(context), nodeId))
                    .header("Authorization", getAuth(context));

            String response = spec.retrieve().body(String.class);
            return ActionResult.success(Map.of("data", response));
        } catch (Exception e) {
            return ActionResult.failure("Failed to fetch Facebook Graph node: " + e.getMessage());
        }
    }
}
