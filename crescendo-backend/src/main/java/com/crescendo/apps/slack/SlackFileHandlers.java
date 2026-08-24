package com.crescendo.apps.slack;

import com.crescendo.execution.action.ActionContext;
import com.crescendo.execution.action.ActionMapping;
import com.crescendo.execution.action.ActionResult;
import com.crescendo.storage.MediaStreamResolver;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.core.io.ByteArrayResource;
import org.springframework.http.MediaType;
import org.springframework.stereotype.Component;
import org.springframework.util.LinkedMultiValueMap;
import org.springframework.util.MultiValueMap;

import java.io.ByteArrayOutputStream;
import java.io.InputStream;
import java.util.Map;

/**
 * Grouped handler for Slack File operations.
 */
@Component
public class SlackFileHandlers {

    private static final String SLACK_API = SlackSupport.SLACK_API;
    private final MediaStreamResolver mediaStreamResolver;

    public SlackFileHandlers() {
        this(new MediaStreamResolver());
    }

    @Autowired
    public SlackFileHandlers(MediaStreamResolver mediaStreamResolver) {
        this.mediaStreamResolver = mediaStreamResolver;
    }

    // ── upload ────────────────────────────────────────────────────────────────
    @ActionMapping(appKey = "slack", actionKey = "uploadFile")
    @SuppressWarnings("unchecked")
    public ActionResult upload(ActionContext context) {
        Map<String, Object> config = context.configuration();
        Object rawFile = config.get("fileContent");
        if (rawFile == null) rawFile = config.get("file");
        if (rawFile == null) rawFile = config.get("fileUrl");

        String fileName = SlackSupport.opt(config, "fileName", null);
        String channel = SlackSupport.opt(config, "channel", null);
        String threadTs = SlackSupport.opt(config, "thread_ts", null);
        String initialComment = SlackSupport.opt(config, "initialComment", null);

        if (rawFile == null) {
            return ActionResult.failure("File or URL is required for Slack file upload");
        }

        try {
            byte[] fileBytes;
            String resolvedFilename = fileName;

            try (MediaStreamResolver.MediaSource media = mediaStreamResolver.resolve(rawFile, "application/octet-stream")) {
                InputStream in = media.stream();
                ByteArrayOutputStream baos = new ByteArrayOutputStream();
                in.transferTo(baos);
                fileBytes = baos.toByteArray();
                if (resolvedFilename == null || resolvedFilename.isBlank()) {
                    resolvedFilename = media.filename() != null ? media.filename() : "upload.dat";
                }
            }

            final String finalFilename = resolvedFilename;
            MultiValueMap<String, Object> body = new LinkedMultiValueMap<>();
            body.add("file", new ByteArrayResource(fileBytes) {
                @Override
                public String getFilename() {
                    return finalFilename;
                }
            });
            if (channel != null) body.add("channels", channel);
            if (threadTs != null) body.add("thread_ts", threadTs);
            if (initialComment != null) body.add("initial_comment", initialComment);

            Map<String, Object> response = SlackSupport.clientBuilder(context).build().post()
                    .uri(SLACK_API + "files.upload")
                    .contentType(MediaType.MULTIPART_FORM_DATA)
                    .body(body)
                    .retrieve()
                    .body(Map.class);

            if (response != null && Boolean.TRUE.equals(response.get("ok"))) {
                return ActionResult.success(response);
            }
            String error = response != null ? String.valueOf(response.get("error")) : "unknown error";
            return ActionResult.failure("Slack files.upload failed: " + error);
        } catch (Exception e) {
            return ActionResult.failure("Slack uploadFile failed: " + e.getMessage());
        }
    }
}
