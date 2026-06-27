package com.crescendo.apps.dropbox;

import com.crescendo.execution.action.ActionContext;
import com.crescendo.execution.action.ActionMapping;
import com.crescendo.execution.action.ActionResult;
import org.springframework.stereotype.Component;
import org.springframework.web.client.RestClient;

import java.util.Map;

@Component
public class DropboxFolderHandlers {

    private String getAuth(ActionContext context) {
        return "Bearer " + context.credentials().get("accessToken");
    }

    @ActionMapping(appKey = "dropbox", actionKey = "create-folder")
    public Object createFolder(ActionContext context) throws Exception {
        String path = context.configuration().get("path") != null ? context.configuration().get("path").toString() : "";
        if (path.isBlank()) return ActionResult.failure("Path is required");

        try {
            String response = RestClient.create("https://api.dropboxapi.com/2")
                    .post()
                    .uri("/files/create_folder_v2")
                    .header("Authorization", getAuth(context))
                    .header("Content-Type", "application/json")
                    .body(Map.of("path", path, "autorename", false))
                    .retrieve()
                    .body(String.class);
            return ActionResult.success(Map.of("data", response));
        } catch (Exception e) {
            return ActionResult.failure("Failed to create Dropbox folder: " + e.getMessage());
        }
    }

    @ActionMapping(appKey = "dropbox", actionKey = "list-folder")
    public Object listFolder(ActionContext context) throws Exception {
        String path = context.configuration().get("path") != null ? context.configuration().get("path").toString() : "";
        if (path.equals("/")) {
            path = "";
        }

        try {
            String response = RestClient.create("https://api.dropboxapi.com/2")
                    .post()
                    .uri("/files/list_folder")
                    .header("Authorization", getAuth(context))
                    .header("Content-Type", "application/json")
                    .body(Map.of("path", path))
                    .retrieve()
                    .body(String.class);
            return ActionResult.success(Map.of("data", response));
        } catch (Exception e) {
            return ActionResult.failure("Failed to list Dropbox folder: " + e.getMessage());
        }
    }
}
