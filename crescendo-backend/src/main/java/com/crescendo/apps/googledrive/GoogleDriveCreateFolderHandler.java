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
 * Creates a new folder in Google Drive via Drive API v3 (files.create).
 */
@ActionMapping(appKey = "google-drive", actionKey = "create-folder")
public class GoogleDriveCreateFolderHandler implements ActionHandler {

    private static final Logger logger = LoggerFactory.getLogger(GoogleDriveCreateFolderHandler.class);
    private static final String DRIVE_API = "https://www.googleapis.com/drive/v3/files";

    private final RestClient restClient;

    public GoogleDriveCreateFolderHandler() {
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

        String folderName = str(config, "folderName");
        String parentFolderId = str(config, "parentFolderId");
        if (folderName == null) return ActionResult.failure("'folderName' is required");

        logger.info("[google-drive] Creating folder: name='{}'", folderName);

        try {
            Map<String, Object> body = new HashMap<>();
            body.put("name", folderName);
            body.put("mimeType", "application/vnd.google-apps.folder");
            if (parentFolderId != null && !parentFolderId.isBlank()) {
                body.put("parents", List.of(parentFolderId));
            }

            Map<String, Object> response = restClient.post()
                    .uri(DRIVE_API)
                    .header(HttpHeaders.AUTHORIZATION, "Bearer " + accessToken)
                    .contentType(MediaType.APPLICATION_JSON)
                    .body(body)
                    .retrieve()
                    .body(Map.class);

            Map<String, Object> output = new HashMap<>();
            output.put("provider", "google-drive");
            output.put("action", "create-folder");
            output.put("folderId", response != null ? response.get("id") : null);
            output.put("folderName", folderName);
            return ActionResult.success(output);

        } catch (Exception e) {
            logger.error("[google-drive] Create folder failed: {}", e.getMessage());
            return ActionResult.failure("Google Drive create-folder failed: " + e.getMessage());
        }
    }

    private String str(Map<String, Object> m, String k) {
        Object v = m.get(k);
        return v != null ? v.toString() : null;
    }
}
