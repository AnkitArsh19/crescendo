package com.crescendo.apps.youtube;

import com.crescendo.execution.action.ActionContext;
import com.crescendo.execution.action.ActionMapping;
import com.crescendo.execution.action.ActionResult;
import org.springframework.stereotype.Component;

import java.util.Map;

/**
 * Grouped handler for YouTube Channel operations.
 */
@Component
public class YouTubeChannelHandlers {

    private static final String BASE = "https://www.googleapis.com/youtube/v3/channels";

    @ActionMapping(appKey = "youtube", actionKey = "getChannel")
    @SuppressWarnings("unchecked")
    public ActionResult get(ActionContext context) {
        String channelId = YouTubeSupport.require(context.configuration(), "channelId");
        if (channelId == null) return ActionResult.failure("'channelId' is required");

        try {
            String uri = BASE + "?part=snippet,contentDetails,statistics&id=" + channelId;
            String apiKey = YouTubeSupport.resolveApiKey(context);
            if (apiKey != null) uri += "&key=" + apiKey;

            Map<String, Object> response = YouTubeSupport.clientBuilder(context).build().get()
                    .uri(uri)
                    .retrieve()
                    .body(Map.class);
            return ActionResult.success(response);
        } catch (Exception e) {
            return ActionResult.failure("YouTube getChannel failed: " + e.getMessage());
        }
    }

    @ActionMapping(appKey = "youtube", actionKey = "getAllChannels")
    @SuppressWarnings("unchecked")
    public ActionResult getAll(ActionContext context) {
        int maxResults = YouTubeSupport.parseIntOpt(context.configuration(), "maxResults", 50);
        String categoryId = YouTubeSupport.opt(context.configuration(), "categoryId", null);
        String forUsername = YouTubeSupport.opt(context.configuration(), "forUsername", null);
        boolean mine = Boolean.parseBoolean(YouTubeSupport.opt(context.configuration(), "mine", "false"));

        try {
            StringBuilder uri = new StringBuilder(BASE + "?part=snippet,contentDetails,statistics&maxResults=" + maxResults);
            if (categoryId != null) uri.append("&categoryId=").append(categoryId);
            if (forUsername != null) uri.append("&forUsername=").append(forUsername);
            if (mine) uri.append("&mine=true");

            String apiKey = YouTubeSupport.resolveApiKey(context);
            if (apiKey != null) uri.append("&key=").append(apiKey);

            Map<String, Object> response = YouTubeSupport.clientBuilder(context).build().get()
                    .uri(uri.toString())
                    .retrieve()
                    .body(Map.class);
            return ActionResult.success(response);
        } catch (Exception e) {
            return ActionResult.failure("YouTube getAllChannels failed: " + e.getMessage());
        }
    }
}
