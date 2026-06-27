package com.crescendo.apps.dropbox;

import com.crescendo.execution.action.ActionContext;
import com.crescendo.execution.action.ActionMapping;
import com.crescendo.execution.action.ActionResult;
import org.springframework.stereotype.Component;
import org.springframework.web.client.RestClient;

import java.util.Map;
import java.util.HashMap;

@Component
public class DropboxSearchHandlers {

    private String getAuth(ActionContext context) {
        return "Bearer " + context.credentials().get("accessToken");
    }

    @ActionMapping(appKey = "dropbox", actionKey = "search")
    public Object search(ActionContext context) throws Exception {
        String query = context.configuration().get("query") != null ? context.configuration().get("query").toString() : "";
        if (query.isBlank()) return ActionResult.failure("Query is required");

        String path = context.configuration().get("path") != null ? context.configuration().get("path").toString() : "";
        if (path.equals("/")) path = "";

        int maxResults = 25;
        if (context.configuration().containsKey("maxResults")) {
            try {
                maxResults = Integer.parseInt(context.configuration().get("maxResults").toString());
            } catch (Exception ignored) {}
        }

        Map<String, Object> body = new HashMap<>();
        body.put("query", query);
        
        Map<String, Object> options = new HashMap<>();
        if (!path.isEmpty()) options.put("path", path);
        options.put("max_results", maxResults);
        body.put("options", options);

        try {
            String response = RestClient.create("https://api.dropboxapi.com/2")
                    .post()
                    .uri("/files/search_v2")
                    .header("Authorization", getAuth(context))
                    .header("Content-Type", "application/json")
                    .body(body)
                    .retrieve()
                    .body(String.class);
            return ActionResult.success(Map.of("data", response));
        } catch (Exception e) {
            return ActionResult.failure("Failed to search Dropbox files: " + e.getMessage());
        }
    }
}
