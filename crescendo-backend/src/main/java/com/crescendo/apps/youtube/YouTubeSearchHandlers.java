package com.crescendo.apps.youtube;

import com.crescendo.execution.action.ActionContext;
import com.crescendo.execution.action.ActionMapping;
import com.crescendo.execution.action.ActionResult;
import org.springframework.stereotype.Component;
import org.springframework.web.util.UriComponentsBuilder;

import java.util.Map;

/**
 * Grouped handler for YouTube Search operations.
 */
@Component
public class YouTubeSearchHandlers {

    private static final String BASE = "https://www.googleapis.com/youtube/v3/search";

    @ActionMapping(appKey = "youtube", actionKey = "search")
    @SuppressWarnings("unchecked")
    public ActionResult search(ActionContext context) {
        try {
            UriComponentsBuilder builder = UriComponentsBuilder.fromUriString(BASE)
                    .queryParam("part", "snippet");

            Map<String, Object> config = context.configuration();

            String query = YouTubeSupport.opt(config, "query", "");
            if (!query.isBlank()) builder.queryParam("q", query);

            builder.queryParam("maxResults", Math.max(1, YouTubeSupport.parseIntOpt(config, "maxResults", 10)));

            String type = YouTubeSupport.opt(config, "type", null); // video, channel, playlist
            if (type != null) builder.queryParam("type", type);

            String apiKey = YouTubeSupport.resolveApiKey(context);
            if (apiKey != null) builder.queryParam("key", apiKey);

            String channelId = YouTubeSupport.opt(config, "channelId", null);
            if (channelId != null) builder.queryParam("channelId", channelId);

            String publishedAfter = YouTubeSupport.opt(config, "publishedAfter", null);
            if (publishedAfter != null) builder.queryParam("publishedAfter", publishedAfter);

            String publishedBefore = YouTubeSupport.opt(config, "publishedBefore", null);
            if (publishedBefore != null) builder.queryParam("publishedBefore", publishedBefore);

            String regionCode = YouTubeSupport.opt(config, "regionCode", null);
            if (regionCode != null) builder.queryParam("regionCode", regionCode);

            String relatedToVideoId = YouTubeSupport.opt(config, "relatedToVideoId", null);
            if (relatedToVideoId != null) builder.queryParam("relatedToVideoId", relatedToVideoId);

            String videoCategoryId = YouTubeSupport.opt(config, "videoCategoryId", null);
            if (videoCategoryId != null) builder.queryParam("videoCategoryId", videoCategoryId);

            String order = YouTubeSupport.opt(config, "order", null);
            if (order != null) builder.queryParam("order", order);

            Map<String, Object> response = YouTubeSupport.clientBuilder(context).build().get()
                    .uri(builder.build().toUriString())
                    .retrieve()
                    .body(Map.class);
            return ActionResult.success(response);
        } catch (Exception e) {
            return ActionResult.failure("YouTube search failed: " + e.getMessage());
        }
    }
}
