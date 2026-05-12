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
import java.util.Map;

/**
 * Copies a file in Google Drive via files.copy.
 */
@ActionMapping(appKey = "google-drive", actionKey = "copy-file")
public class GoogleDriveCopyFileHandler implements ActionHandler {

    private static final Logger logger = LoggerFactory.getLogger(GoogleDriveCopyFileHandler.class);
    private static final String DRIVE_API = "https://www.googleapis.com/drive/v3/files/";

    private final RestClient restClient;

    public GoogleDriveCopyFileHandler() {
        this.restClient = RestClient.create();
    }

    @Override
    @SuppressWarnings("unchecked")
    public ActionResult execute(ActionContext context) {
        Map<String, Object> config = context.configuration();
        Map<String, Object> creds = context.credentials();

        String accessToken = creds != null ? (String) creds.get("accessToken") : null;
        if (accessToken == null || accessToken.isBlank()) {
            return ActionResult.failure("Google Drive requires an OAuth2 accessToken");
        }

        String fileId = str(config, "fileId");
        String newName = str(config, "newName");
        if (fileId == null) return ActionResult.failure("'fileId' is required");

        logger.info("[google-drive] Copying file '{}' as '{}'", fileId, newName);

        try {
            Map<String, Object> body = new HashMap<>();
            if (newName != null && !newName.isBlank()) {
                body.put("name", newName);
            }
            String destFolder = str(config, "destinationFolderId");
            if (destFolder != null && !destFolder.isBlank()) {
                body.put("parents", java.util.List.of(destFolder));
            }

            Map<String, Object> response = restClient.post()
                    .uri(DRIVE_API + fileId + "/copy")
                    .header(HttpHeaders.AUTHORIZATION, "Bearer " + accessToken)
                    .contentType(MediaType.APPLICATION_JSON)
                    .body(body)
                    .retrieve()
                    .body(Map.class);

            Map<String, Object> output = new HashMap<>();
            output.put("provider", "google-drive");
            output.put("action", "copy-file");
            output.put("originalFileId", fileId);
            output.put("newFileId", response != null ? response.get("id") : null);
            output.put("newName", response != null ? response.get("name") : newName);
            return ActionResult.success(output);

        } catch (Exception e) {
            logger.error("[google-drive] Copy file failed: {}", e.getMessage());
            return ActionResult.failure("Google Drive copy-file failed: " + e.getMessage());
        }
    }

    private String str(Map<String, Object> m, String k) {
        Object v = m.get(k);
        return v != null ? v.toString() : null;
    }
}
