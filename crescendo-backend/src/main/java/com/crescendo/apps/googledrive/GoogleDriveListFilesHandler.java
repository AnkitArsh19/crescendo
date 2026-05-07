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

@ActionMapping(appKey = "google-drive", actionKey = "list-files")
public class GoogleDriveListFilesHandler implements ActionHandler {

    private static final Logger logger = LoggerFactory.getLogger(GoogleDriveListFilesHandler.class);
    private static final String DRIVE_API = "https://www.googleapis.com/drive/v3/files";

    @Override
    public ActionResult execute(ActionContext context) {
        Map<String, Object> config = context.configuration();
        Map<String, Object> creds = context.credentials();

        String token = creds != null ? (String) creds.get("accessToken") : null;
        if (token == null || token.isBlank()) {
            return ActionResult.failure("Google Drive requires 'accessToken' in connection credentials");
        }

        int pageSize = 20;
        if (config.containsKey("pageSize")) {
            try { pageSize = Integer.parseInt(config.get("pageSize").toString()); }
            catch (NumberFormatException ignored) {}
        }

        String query = config.get("query") != null ? config.get("query").toString() : null;

        try {
            String uri = DRIVE_API + "?pageSize=" + pageSize + "&fields=files(id,name,mimeType,modifiedTime,size)";
            if (query != null && !query.isBlank()) {
                uri += "&q=" + query;
            }

            String response = RestClient.create()
                    .get()
                    .uri(uri)
                    .header(HttpHeaders.AUTHORIZATION, "Bearer " + token)
                    .retrieve()
                    .body(String.class);

            Map<String, Object> output = new HashMap<>();
            output.put("response", response);
            logger.info("[google-drive] Files listed successfully");
            return ActionResult.success(output);
        } catch (Exception e) {
            logger.error("[google-drive] List files failed", e);
            return ActionResult.failure("Google Drive list files failed: " + e.getMessage());
        }
    }
}
