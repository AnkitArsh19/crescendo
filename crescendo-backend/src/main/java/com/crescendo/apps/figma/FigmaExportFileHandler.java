package com.crescendo.apps.figma;

import com.crescendo.execution.action.*;
import org.springframework.http.HttpHeaders;
import org.springframework.web.client.RestClient;
import java.util.*;

/**
 * Exports a Figma file or specific nodes as images via GET /v1/images/{key}.
 */
@ActionMapping(appKey = "figma", actionKey = "export-file")
public class FigmaExportFileHandler implements ActionHandler {
    private final RestClient restClient = RestClient.create();

    @Override
    @SuppressWarnings("unchecked")
    public ActionResult execute(ActionContext context) {
        Map<String, Object> config = context.configuration();
        Map<String, Object> creds = context.credentials();
        String token = creds != null ? (String) creds.get("accessToken") : null;
        if (token == null)
            return ActionResult.failure("Figma requires an OAuth2 accessToken");

        String fileKey = config.get("fileKey") != null ? config.get("fileKey").toString() : null;
        if (fileKey == null)
            return ActionResult.failure("'fileKey' is required");

        // Extract key from URL if full URL provided
        if (fileKey.contains("figma.com")) {
            String[] parts = fileKey.split("/");
            for (int i = 0; i < parts.length; i++) {
                if ("design".equals(parts[i]) || "file".equals(parts[i])) {
                    if (i + 1 < parts.length) {
                        fileKey = parts[i + 1];
                        break;
                    }
                }
            }
        }

        String nodeIds = config.get("nodeIds") != null ? config.get("nodeIds").toString() : null;
        String format = config.getOrDefault("format", "png").toString().toLowerCase();
        String scale = config.getOrDefault("scale", "1").toString();

        try {
            StringBuilder url = new StringBuilder(
                    "https://api.figma.com/v1/images/" + fileKey + "?format=" + format + "&scale=" + scale);
            if (nodeIds != null && !nodeIds.isBlank())
                url.append("&ids=").append(nodeIds);

            Map<String, Object> resp = restClient.get()
                    .uri(url.toString())
                    .header(HttpHeaders.AUTHORIZATION, "Bearer " + token)
                    .retrieve().body(Map.class);

            Map<String, Object> out = new HashMap<>();
            out.put("provider", "figma");
            out.put("action", "export-file");
            out.put("fileKey", fileKey);
            out.put("format", format);
            out.put("images", resp != null ? resp.get("images") : null);
            return ActionResult.success(out);
        } catch (Exception e) {
            return ActionResult.failure("Figma export failed: " + e.getMessage());
        }
    }
}
