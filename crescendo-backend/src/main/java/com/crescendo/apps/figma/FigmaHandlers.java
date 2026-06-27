package com.crescendo.apps.figma;

import com.crescendo.execution.action.ActionContext;
import com.crescendo.execution.action.ActionMapping;
import com.crescendo.execution.action.ActionResult;
import org.springframework.stereotype.Component;
import org.springframework.web.client.RestClient;

import java.util.Map;
// import java.util.HashMap;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

@Component
public class FigmaHandlers {

    private static final String FIGMA_API_BASE = "https://api.figma.com/v1";

    private String getAuth(ActionContext context) {
        return "Bearer " + context.credentials().get("accessToken");
    }

    private String extractFileKey(String input) {
        if (input == null || input.isBlank()) return "";
        Matcher m = Pattern.compile("figma\\.com/(?:file|design)/([a-zA-Z0-9]+)").matcher(input);
        return m.find() ? m.group(1) : input.trim();
    }

    @ActionMapping(appKey = "figma", actionKey = "get-file")
    public Object getFile(ActionContext context) throws Exception {
        String fileKeyRaw = context.configuration().get("fileKey") != null ? context.configuration().get("fileKey").toString() : "";
        String fileKey = extractFileKey(fileKeyRaw);
        if (fileKey.isBlank()) return ActionResult.failure("Figma File Key is required");

        try {
            String response = RestClient.create(FIGMA_API_BASE)
                    .get()
                    .uri("/files/{fileKey}", fileKey)
                    .header("Authorization", getAuth(context))
                    .retrieve()
                    .body(String.class);
            return ActionResult.success(Map.of("data", response));
        } catch (Exception e) {
            return ActionResult.failure("Failed to fetch Figma file: " + e.getMessage());
        }
    }

    @ActionMapping(appKey = "figma", actionKey = "post-comment")
    public Object postComment(ActionContext context) throws Exception {
        String fileKeyRaw = context.configuration().get("fileKey") != null ? context.configuration().get("fileKey").toString() : "";
        String fileKey = extractFileKey(fileKeyRaw);
        String message = context.configuration().get("message") != null ? context.configuration().get("message").toString() : "";

        if (fileKey.isBlank() || message.isBlank()) return ActionResult.failure("File Key and Message are required");

        try {
            String response = RestClient.create(FIGMA_API_BASE)
                    .post()
                    .uri("/files/{fileKey}/comments", fileKey)
                    .header("Authorization", getAuth(context))
                    .header("Content-Type", "application/json")
                    .body(Map.of("message", message))
                    .retrieve()
                    .body(String.class);
            return ActionResult.success(Map.of("data", response));
        } catch (Exception e) {
            return ActionResult.failure("Failed to post Figma comment: " + e.getMessage());
        }
    }

    @ActionMapping(appKey = "figma", actionKey = "list-comments")
    public Object listComments(ActionContext context) throws Exception {
        String fileKeyRaw = context.configuration().get("fileKey") != null ? context.configuration().get("fileKey").toString() : "";
        String fileKey = extractFileKey(fileKeyRaw);
        if (fileKey.isBlank()) return ActionResult.failure("Figma File Key is required");

        try {
            String response = RestClient.create(FIGMA_API_BASE)
                    .get()
                    .uri("/files/{fileKey}/comments", fileKey)
                    .header("Authorization", getAuth(context))
                    .retrieve()
                    .body(String.class);
            return ActionResult.success(Map.of("data", response));
        } catch (Exception e) {
            return ActionResult.failure("Failed to list Figma comments: " + e.getMessage());
        }
    }

    @ActionMapping(appKey = "figma", actionKey = "export-file")
    public Object exportFile(ActionContext context) throws Exception {
        String fileKeyRaw = context.configuration().get("fileKey") != null ? context.configuration().get("fileKey").toString() : "";
        String fileKey = extractFileKey(fileKeyRaw);
        if (fileKey.isBlank()) return ActionResult.failure("Figma File Key is required");

        String nodeIds = context.configuration().get("nodeIds") != null ? context.configuration().get("nodeIds").toString() : "";
        String format = context.configuration().get("format") != null ? context.configuration().get("format").toString() : "png";
        String scale = context.configuration().get("scale") != null ? context.configuration().get("scale").toString() : "1";

        try {
            RestClient.RequestHeadersSpec<?> spec = RestClient.create(FIGMA_API_BASE)
                    .get()
                    .uri(uriBuilder -> uriBuilder
                        .path("/images/{fileKey}")
                        .queryParamIfPresent("ids", java.util.Optional.ofNullable(nodeIds.isBlank() ? null : nodeIds))
                        .queryParam("format", format)
                        .queryParam("scale", scale)
                        .build(fileKey))
                    .header("Authorization", getAuth(context));

            String response = spec.retrieve().body(String.class);
            return ActionResult.success(Map.of("data", response));
        } catch (Exception e) {
            return ActionResult.failure("Failed to export Figma file: " + e.getMessage());
        }
    }
}
