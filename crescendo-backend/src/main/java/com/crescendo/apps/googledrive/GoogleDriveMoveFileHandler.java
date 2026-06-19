package com.crescendo.apps.googledrive;

import com.crescendo.execution.action.ActionContext;
import com.crescendo.execution.action.ActionHandler;
import com.crescendo.execution.action.ActionMapping;
import com.crescendo.execution.action.ActionResult;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.http.HttpHeaders;
import org.springframework.http.MediaType;
import org.springframework.web.client.RestClient;

import java.util.HashMap;
import java.util.List;
import java.util.Map;

/**
 * Moves a file to a different folder in Google Drive via files.update
 * (addParents/removeParents).
 */
@ActionMapping(appKey = "google-drive", actionKey = "move-file")
@SuppressWarnings("unchecked")
public class GoogleDriveMoveFileHandler implements ActionHandler {

    private static final Logger logger = LoggerFactory.getLogger(GoogleDriveMoveFileHandler.class);
    private static final String DRIVE_API = "https://www.googleapis.com/drive/v3/files/";

    private final RestClient restClient;

    public GoogleDriveMoveFileHandler() {
        this.restClient = RestClient.create();
    }

    @Override
    public ActionResult execute(ActionContext context) {
        Map<String, Object> config = context.configuration();
        Map<String, Object> creds = context.credentials();

        String accessToken = creds != null ? (String) creds.get("accessToken") : null;
        if (accessToken == null || accessToken.isBlank()) {
            return ActionResult.failure("Google Drive requires an OAuth2 accessToken");
        }

        String fileId = str(config, "fileId");
        String destinationFolderId = str(config, "destinationFolderId");
        if (fileId == null)
            return ActionResult.failure("'fileId' is required");
        if (destinationFolderId == null)
            return ActionResult.failure("'destinationFolderId' is required");

        logger.info("[google-drive] Moving file '{}' to folder '{}'", fileId, destinationFolderId);

        try {
            // Get current parents
            Map<String, Object> fileMeta = restClient.get()
                    .uri(DRIVE_API + fileId + "?fields=parents")
                    .header(HttpHeaders.AUTHORIZATION, "Bearer " + accessToken)
                    .retrieve()
                    .body(Map.class);

            List<String> previousParents = fileMeta != null ? (List<String>) fileMeta.get("parents") : List.of();
            String removeParents = String.join(",", previousParents);

            // Move file
            String url = DRIVE_API + fileId + "?addParents=" + destinationFolderId
                    + "&removeParents=" + removeParents + "&fields=id,parents";

            restClient.patch()
                    .uri(url)
                    .header(HttpHeaders.AUTHORIZATION, "Bearer " + accessToken)
                    .contentType(MediaType.APPLICATION_JSON)
                    .body(Map.of())
                    .retrieve()
                    .body(Map.class);

            Map<String, Object> output = new HashMap<>();
            output.put("provider", "google-drive");
            output.put("action", "move-file");
            output.put("fileId", fileId);
            output.put("newParent", destinationFolderId);
            output.put("previousParents", previousParents);
            return ActionResult.success(output);

        } catch (Exception e) {
            logger.error("[google-drive] Move file failed: {}", e.getMessage());
            return ActionResult.failure("Google Drive move-file failed: " + e.getMessage());
        }
    }

    private String str(Map<String, Object> m, String k) {
        Object v = m.get(k);
        return v != null ? v.toString() : null;
    }
}
