package com.crescendo.apps.figma;

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

@ActionMapping(appKey = "figma", actionKey = "post-comment")
public class FigmaPostCommentHandler implements ActionHandler {

    private static final Logger logger = LoggerFactory.getLogger(FigmaPostCommentHandler.class);
    private static final String FIGMA_API = "https://api.figma.com/v1";

    @Override
    public ActionResult execute(ActionContext context) {
        Map<String, Object> config = context.configuration();
        Map<String, Object> creds = context.credentials();

        String token = resolveToken(creds);
        if (token == null) return ActionResult.failure("Figma requires an access token in connection credentials");

        String fileInput = config.get("fileKey") != null ? config.get("fileKey").toString().trim() : null;
        String message = config.get("message") != null ? config.get("message").toString() : null;

        if (fileInput == null || fileInput.isBlank()) return ActionResult.failure("'fileKey' is required");
        if (message == null || message.isBlank()) return ActionResult.failure("'message' is required");

        String fileKey = FigmaGetFileHandler.extractFileKey(fileInput);

        try {
            String response = RestClient.create()
                    .post()
                    .uri(FIGMA_API + "/files/" + fileKey + "/comments")
                    .header(HttpHeaders.AUTHORIZATION, "Bearer " + token)
                    .contentType(MediaType.APPLICATION_JSON)
                    .body(Map.of("message", message))
                    .retrieve()
                    .body(String.class);

            Map<String, Object> output = new HashMap<>();
            output.put("response", response);
            logger.info("[figma] Comment posted successfully");
            return ActionResult.success(output);
        } catch (Exception e) {
            logger.error("[figma] Post comment failed", e);
            return ActionResult.failure("Figma post comment failed: " + e.getMessage());
        }
    }

    private String resolveToken(Map<String, Object> creds) {
        if (creds == null) return null;
        Object token = creds.get("accessToken");
        if (token != null) return token.toString();
        Object apiKey = creds.get("apiKey");
        return apiKey != null ? apiKey.toString() : null;
    }
}
