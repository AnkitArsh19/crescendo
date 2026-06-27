package com.crescendo.apps.youtube;

import com.crescendo.execution.action.ActionContext;
import com.crescendo.execution.action.ActionMapping;
import com.crescendo.execution.action.ActionResult;
import org.springframework.stereotype.Component;

import java.util.Map;

/**
 * Grouped handler for YouTube Video Category operations.
 */
@Component
public class YouTubeVideoCategoryHandlers {

    private static final String BASE = "https://www.googleapis.com/youtube/v3/videoCategories";

    @ActionMapping(appKey = "youtube", actionKey = "getAllVideoCategories")
    @SuppressWarnings("unchecked")
    public ActionResult getAll(ActionContext context) {
        String regionCode = YouTubeSupport.opt(context.configuration(), "regionCode", "US");

        try {
            StringBuilder uri = new StringBuilder(BASE + "?part=snippet&regionCode=" + regionCode);

            String apiKey = YouTubeSupport.resolveApiKey(context);
            if (apiKey != null) uri.append("&key=").append(apiKey);

            Map<String, Object> response = YouTubeSupport.clientBuilder(context).build().get()
                    .uri(uri.toString())
                    .retrieve()
                    .body(Map.class);
            return ActionResult.success(response);
        } catch (Exception e) {
            return ActionResult.failure("YouTube getAllVideoCategories failed: " + e.getMessage());
        }
    }
}
