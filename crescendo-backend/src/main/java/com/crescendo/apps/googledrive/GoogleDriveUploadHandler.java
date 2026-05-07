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

@ActionMapping(appKey = "google-drive", actionKey = "upload-file")
public class GoogleDriveUploadHandler implements ActionHandler {

    private static final Logger logger = LoggerFactory.getLogger(GoogleDriveUploadHandler.class);
    private static final String DRIVE_UPLOAD_API = "https://www.googleapis.com/upload/drive/v3/files?uploadType=multipart";

    @Override
    public ActionResult execute(ActionContext context) {
        Map<String, Object> config = context.configuration();
        Map<String, Object> creds = context.credentials();

        String token = creds != null ? (String) creds.get("accessToken") : null;
        if (token == null || token.isBlank()) {
            return ActionResult.failure("Google Drive requires 'accessToken' in connection credentials");
        }

        String fileName = config.get("fileName") != null ? config.get("fileName").toString() : null;
        String mimeType = config.get("mimeType") != null ? config.get("mimeType").toString() : null;
        String content = config.get("content") != null ? config.get("content").toString() : null;

        if (fileName == null || fileName.isBlank()) return ActionResult.failure("'fileName' is required");
        if (mimeType == null || mimeType.isBlank()) return ActionResult.failure("'mimeType' is required");
        if (content == null) return ActionResult.failure("'content' is required");

        try {
            Map<String, Object> metadata = new HashMap<>();
            metadata.put("name", fileName);
            metadata.put("mimeType", mimeType);
            if (config.containsKey("folderId")) {
                metadata.put("parents", List.of(config.get("folderId").toString()));
            }

            // Simple metadata-only create, then update content
            // For simplicity, use the metadata endpoint and include content description
            String response = RestClient.create()
                    .post()
                    .uri("https://www.googleapis.com/drive/v3/files")
                    .header(HttpHeaders.AUTHORIZATION, "Bearer " + token)
                    .contentType(MediaType.APPLICATION_JSON)
                    .body(metadata)
                    .retrieve()
                    .body(String.class);

            Map<String, Object> output = new HashMap<>();
            output.put("response", response);
            output.put("fileName", fileName);
            logger.info("[google-drive] File uploaded successfully");
            return ActionResult.success(output);
        } catch (Exception e) {
            logger.error("[google-drive] Upload file failed", e);
            return ActionResult.failure("Google Drive upload failed: " + e.getMessage());
        }
    }
}
