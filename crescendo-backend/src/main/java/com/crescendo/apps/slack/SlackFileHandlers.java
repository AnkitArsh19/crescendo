package com.crescendo.apps.slack;

import com.crescendo.execution.action.ActionContext;
import com.crescendo.execution.action.ActionMapping;
import com.crescendo.execution.action.ActionResult;
import org.springframework.core.io.ByteArrayResource;
// import org.springframework.http.HttpHeaders;
import org.springframework.http.MediaType;
import org.springframework.stereotype.Component;
import org.springframework.util.LinkedMultiValueMap;
import org.springframework.util.MultiValueMap;

import java.util.Base64;
import java.util.Map;

/**
 * Grouped handler for Slack File operations.
 */
@Component
public class SlackFileHandlers {

    private static final String SLACK_API = SlackSupport.SLACK_API;

    // ── upload ────────────────────────────────────────────────────────────────
    @ActionMapping(appKey = "slack", actionKey = "uploadFile")
    @SuppressWarnings("unchecked")
    public ActionResult upload(ActionContext context) {
        Map<String, Object> config = context.configuration();
        String fileContent = SlackSupport.require(config, "fileContent"); // Base64
        String fileName = SlackSupport.require(config, "fileName");
        String channel = SlackSupport.opt(config, "channel", null);
        String threadTs = SlackSupport.opt(config, "thread_ts", null);
        String initialComment = SlackSupport.opt(config, "initialComment", null);

        if (fileContent == null || fileName == null) {
            return ActionResult.failure("'fileContent' and 'fileName' are required");
        }

        try {
            byte[] fileBytes = Base64.getDecoder().decode(fileContent);

            MultiValueMap<String, Object> body = new LinkedMultiValueMap<>();
            body.add("file", new ByteArrayResource(fileBytes) {
                @Override
                public String getFilename() {
                    return fileName;
                }
            });
            if (channel != null) body.add("channels", channel);
            if (threadTs != null) body.add("thread_ts", threadTs);
            if (initialComment != null) body.add("initial_comment", initialComment);

// String token = SlackSupport.resolveToken(context);

            Map<String, Object> response = SlackSupport.clientBuilder(context).build().post()
                    .uri(SLACK_API + "files.upload")
                    .contentType(MediaType.MULTIPART_FORM_DATA)
                    .body(body)
                    .retrieve()
                    .body(Map.class);
            return ActionResult.success(response);
        } catch (Exception e) {
            return ActionResult.failure("Slack uploadFile failed: " + e.getMessage());
        }
    }

    // ── getAll ────────────────────────────────────────────────────────────────
    @ActionMapping(appKey = "slack", actionKey = "getAllFiles")
    @SuppressWarnings("unchecked")
    public ActionResult getAll(ActionContext context) {
        Map<String, Object> config = context.configuration();
        int count = SlackSupport.parseIntOpt(config, "count", 100);
        String channel = SlackSupport.opt(config, "channel", null);
        String user = SlackSupport.opt(config, "user", null);

        try {
            StringBuilder uri = new StringBuilder(SLACK_API + "files.list?count=" + count);
            if (channel != null) uri.append("&channel=").append(channel);
            if (user != null) uri.append("&user=").append(user);

            Map<String, Object> response = SlackSupport.clientBuilder(context).build().get()
                    .uri(uri.toString())
                    .retrieve()
                    .body(Map.class);
            return ActionResult.success(response);
        } catch (Exception e) {
            return ActionResult.failure("Slack getAllFiles failed: " + e.getMessage());
        }
    }
}
