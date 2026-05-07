package com.crescendo.apps.figma;

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

@ActionMapping(appKey = "figma", actionKey = "get-file")
public class FigmaGetFileHandler implements ActionHandler {

    private static final Logger logger = LoggerFactory.getLogger(FigmaGetFileHandler.class);
    private static final String FIGMA_API = "https://api.figma.com/v1";

    @Override
    public ActionResult execute(ActionContext context) {
        Map<String, Object> config = context.configuration();
        Map<String, Object> creds = context.credentials();

        String token = resolveToken(creds);
        if (token == null) return ActionResult.failure("Figma requires an access token in connection credentials");

        String fileInput = config.get("fileKey") != null ? config.get("fileKey").toString().trim() : null;
        if (fileInput == null || fileInput.isBlank()) return ActionResult.failure("'fileKey' is required");

        // Extract file key from URL if a full Figma URL was provided
        String fileKey = extractFileKey(fileInput);

        try {
            String response = RestClient.create()
                    .get()
                    .uri(FIGMA_API + "/files/" + fileKey)
                    .header(HttpHeaders.AUTHORIZATION, "Bearer " + token)
                    .retrieve()
                    .body(String.class);

            Map<String, Object> output = new HashMap<>();
            output.put("response", response);
            output.put("fileKey", fileKey);
            logger.info("[figma] File fetched successfully");
            return ActionResult.success(output);
        } catch (Exception e) {
            logger.error("[figma] Get file failed for {}", fileKey, e);
            return ActionResult.failure("Figma get file failed: " + e.getMessage());
        }
    }

    private String resolveToken(Map<String, Object> creds) {
        if (creds == null) return null;
        Object token = creds.get("accessToken");
        if (token != null) return token.toString();
        Object apiKey = creds.get("apiKey");
        return apiKey != null ? apiKey.toString() : null;
    }

    /**
     * Extracts the Figma file key from a URL or returns the input as-is.
     * Handles URLs like:
     * - https://www.figma.com/design/ABC123xyz/My-Design
     * - https://www.figma.com/file/ABC123xyz/My-Design
     */
    static String extractFileKey(String input) {
        if (input == null) return null;
        input = input.trim();
        // Match patterns: /file/{key}/ or /design/{key}/
        if (input.contains("figma.com/")) {
            String[] parts = input.split("/");
            for (int i = 0; i < parts.length - 1; i++) {
                if (parts[i].equals("file") || parts[i].equals("design")) {
                    return parts[i + 1].split("\\?")[0]; // Remove query params
                }
            }
        }
        return input;
    }
}
