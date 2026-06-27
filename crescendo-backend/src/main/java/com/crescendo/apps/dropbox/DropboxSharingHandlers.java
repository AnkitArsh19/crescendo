package com.crescendo.apps.dropbox;

import com.crescendo.execution.action.ActionContext;
import com.crescendo.execution.action.ActionMapping;
import com.crescendo.execution.action.ActionResult;
import org.springframework.stereotype.Component;
import org.springframework.web.client.RestClient;

import java.util.Map;
import java.util.HashMap;

@Component
public class DropboxSharingHandlers {

    private String getAuth(ActionContext context) {
        return "Bearer " + context.credentials().get("accessToken");
    }

    @ActionMapping(appKey = "dropbox", actionKey = "create-shared-link")
    public Object createSharedLink(ActionContext context) throws Exception {
        String path = context.configuration().get("path") != null ? context.configuration().get("path").toString() : "";
        if (path.isBlank()) return ActionResult.failure("Path is required");

        Map<String, Object> settings = new HashMap<>();
        String audience = context.configuration().get("audience") != null ? context.configuration().get("audience").toString() : "";
        if (!audience.isBlank()) {
            settings.put("requested_visibility", audience);
        }
        
        String access = context.configuration().get("access") != null ? context.configuration().get("access").toString() : "";
        if (!access.isBlank()) {
            settings.put("access", access);
        }

        Map<String, Object> body = new HashMap<>();
        body.put("path", path);
        if (!settings.isEmpty()) {
            body.put("settings", settings);
        }

        try {
            String response = RestClient.create("https://api.dropboxapi.com/2")
                    .post()
                    .uri("/sharing/create_shared_link_with_settings")
                    .header("Authorization", getAuth(context))
                    .header("Content-Type", "application/json")
                    .body(body)
                    .retrieve()
                    .body(String.class);
            return ActionResult.success(Map.of("data", response));
        } catch (Exception e) {
            return ActionResult.failure("Failed to create Dropbox shared link: " + e.getMessage());
        }
    }
}
