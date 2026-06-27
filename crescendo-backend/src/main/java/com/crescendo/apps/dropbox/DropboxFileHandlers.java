package com.crescendo.apps.dropbox;

import com.crescendo.execution.action.ActionContext;
import com.crescendo.execution.action.ActionMapping;
import com.crescendo.execution.action.ActionResult;
import org.springframework.stereotype.Component;
import org.springframework.web.client.RestClient;

import java.util.Base64;
import java.util.Map;

@Component
public class DropboxFileHandlers {

    private String getAuth(ActionContext context) {
        return "Bearer " + context.credentials().get("accessToken");
    }

    @ActionMapping(appKey = "dropbox", actionKey = "upload-text")
    public Object uploadText(ActionContext context) throws Exception {
        String path = context.configuration().get("path") != null ? context.configuration().get("path").toString() : "";
        String content = context.configuration().get("content") != null ? context.configuration().get("content").toString() : "";

        if (path.isBlank() || content.isBlank()) return ActionResult.failure("Path and content are required");

        String apiArg = "{\"path\":\"" + path + "\",\"mode\":\"add\",\"autorename\":true,\"mute\":false,\"strict_conflict\":false}";

        try {
            String response = RestClient.create("https://content.dropboxapi.com/2")
                    .post()
                    .uri("/files/upload")
                    .header("Authorization", getAuth(context))
                    .header("Dropbox-API-Arg", apiArg)
                    .header("Content-Type", "application/octet-stream")
                    .body(content)
                    .retrieve()
                    .body(String.class);
            return ActionResult.success(Map.of("data", response));
        } catch (Exception e) {
            return ActionResult.failure("Failed to upload text to Dropbox: " + e.getMessage());
        }
    }

    @ActionMapping(appKey = "dropbox", actionKey = "download")
    public Object downloadFile(ActionContext context) throws Exception {
        String path = context.configuration().get("path") != null ? context.configuration().get("path").toString() : "";
        if (path.isBlank()) return ActionResult.failure("Path is required");

        String apiArg = "{\"path\":\"" + path + "\"}";

        try {
            byte[] response = RestClient.create("https://content.dropboxapi.com/2")
                    .post()
                    .uri("/files/download")
                    .header("Authorization", getAuth(context))
                    .header("Dropbox-API-Arg", apiArg)
                    .retrieve()
                    .body(byte[].class);
            
            if (response == null) return ActionResult.failure("Empty response");
            return ActionResult.success(Map.of("base64", Base64.getEncoder().encodeToString(response)));
        } catch (Exception e) {
            return ActionResult.failure("Failed to download Dropbox file: " + e.getMessage());
        }
    }

    @ActionMapping(appKey = "dropbox", actionKey = "delete")
    public Object deleteFile(ActionContext context) throws Exception {
        String path = context.configuration().get("path") != null ? context.configuration().get("path").toString() : "";
        if (path.isBlank()) return ActionResult.failure("Path is required");

        try {
            String response = RestClient.create("https://api.dropboxapi.com/2")
                    .post()
                    .uri("/files/delete_v2")
                    .header("Authorization", getAuth(context))
                    .header("Content-Type", "application/json")
                    .body(Map.of("path", path))
                    .retrieve()
                    .body(String.class);
            return ActionResult.success(Map.of("data", response));
        } catch (Exception e) {
            return ActionResult.failure("Failed to delete Dropbox file/folder: " + e.getMessage());
        }
    }

    @ActionMapping(appKey = "dropbox", actionKey = "list-revisions")
    public Object listRevisions(ActionContext context) throws Exception {
        String path = context.configuration().get("path") != null ? context.configuration().get("path").toString() : "";
        if (path.isBlank()) return ActionResult.failure("Path is required");
        
        int limit = 10;
        if (context.configuration().containsKey("limit")) {
            try {
                limit = Integer.parseInt(context.configuration().get("limit").toString());
            } catch (Exception ignored) {}
        }

        try {
            String response = RestClient.create("https://api.dropboxapi.com/2")
                    .post()
                    .uri("/files/list_revisions")
                    .header("Authorization", getAuth(context))
                    .header("Content-Type", "application/json")
                    .body(Map.of("path", path, "limit", limit))
                    .retrieve()
                    .body(String.class);
            return ActionResult.success(Map.of("data", response));
        } catch (Exception e) {
            return ActionResult.failure("Failed to list Dropbox file revisions: " + e.getMessage());
        }
    }

    @ActionMapping(appKey = "dropbox", actionKey = "restore-revision")
    public Object restoreRevision(ActionContext context) throws Exception {
        String path = context.configuration().get("path") != null ? context.configuration().get("path").toString() : "";
        String rev = context.configuration().get("rev") != null ? context.configuration().get("rev").toString() : "";
        if (path.isBlank() || rev.isBlank()) return ActionResult.failure("Path and Revision ID are required");

        try {
            String response = RestClient.create("https://api.dropboxapi.com/2")
                    .post()
                    .uri("/files/restore")
                    .header("Authorization", getAuth(context))
                    .header("Content-Type", "application/json")
                    .body(Map.of("path", path, "rev", rev))
                    .retrieve()
                    .body(String.class);
            return ActionResult.success(Map.of("data", response));
        } catch (Exception e) {
            return ActionResult.failure("Failed to restore Dropbox file revision: " + e.getMessage());
        }
    }
}
