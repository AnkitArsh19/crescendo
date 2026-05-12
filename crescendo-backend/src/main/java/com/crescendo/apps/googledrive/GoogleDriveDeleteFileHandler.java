package com.crescendo.apps.googledrive;

import com.crescendo.execution.action.ActionContext;
import com.crescendo.execution.action.ActionHandler;
import com.crescendo.execution.action.ActionMapping;
import com.crescendo.execution.action.ActionResult;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.http.HttpHeaders;
import org.springframework.web.client.RestClient;

import java.util.HashMap;
import java.util.Map;

/**
 * Deletes a file from Google Drive via files.delete.
 */
@ActionMapping(appKey = "google-drive", actionKey = "delete-file")
public class GoogleDriveDeleteFileHandler implements ActionHandler {

    private static final Logger logger = LoggerFactory.getLogger(GoogleDriveDeleteFileHandler.class);
    private static final String DRIVE_API = "https://www.googleapis.com/drive/v3/files/";

    private final RestClient restClient;

    public GoogleDriveDeleteFileHandler() {
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
        if (fileId == null) return ActionResult.failure("'fileId' is required");

        logger.info("[google-drive] Deleting file '{}'", fileId);

        try {
            restClient.delete()
                    .uri(DRIVE_API + fileId)
                    .header(HttpHeaders.AUTHORIZATION, "Bearer " + accessToken)
                    .retrieve()
                    .toBodilessEntity();

            Map<String, Object> output = new HashMap<>();
            output.put("provider", "google-drive");
            output.put("action", "delete-file");
            output.put("fileId", fileId);
            output.put("deleted", true);
            return ActionResult.success(output);

        } catch (Exception e) {
            logger.error("[google-drive] Delete file failed: {}", e.getMessage());
            return ActionResult.failure("Google Drive delete-file failed: " + e.getMessage());
        }
    }

    private String str(Map<String, Object> m, String k) {
        Object v = m.get(k);
        return v != null ? v.toString() : null;
    }
}
