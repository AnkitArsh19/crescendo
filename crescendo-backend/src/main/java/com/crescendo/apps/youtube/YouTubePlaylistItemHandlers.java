package com.crescendo.apps.youtube;

import com.crescendo.execution.action.ActionContext;
import com.crescendo.execution.action.ActionMapping;
import com.crescendo.execution.action.ActionResult;
import org.springframework.stereotype.Component;

import java.util.Map;

/**
 * Grouped handler for YouTube Playlist Item operations.
 */
@Component
public class YouTubePlaylistItemHandlers {

    private static final String BASE = "https://www.googleapis.com/youtube/v3/playlistItems";

    @ActionMapping(appKey = "youtube", actionKey = "addPlaylistItem")
    @SuppressWarnings("unchecked")
    public ActionResult add(ActionContext context) {
        String playlistId = YouTubeSupport.require(context.configuration(), "playlistId");
        String videoId = YouTubeSupport.require(context.configuration(), "videoId");
        if (playlistId == null || videoId == null) {
            return ActionResult.failure("'playlistId' and 'videoId' are required");
        }

        if (YouTubeSupport.resolveToken(context) == null) {
            return ActionResult.failure("YouTube addPlaylistItem requires an OAuth accessToken");
        }

        try {
            Map<String, Object> body = Map.of(
                    "snippet", Map.of(
                            "playlistId", playlistId,
                            "resourceId", Map.of(
                                    "kind", "youtube#video",
                                    "videoId", videoId
                            )
                    )
            );

            Map<String, Object> response = YouTubeSupport.clientBuilder(context).build().post()
                    .uri(BASE + "?part=snippet")
                    .body(body)
                    .retrieve()
                    .body(Map.class);
            return ActionResult.success(response);
        } catch (Exception e) {
            return ActionResult.failure("YouTube addPlaylistItem failed: " + e.getMessage());
        }
    }

    @ActionMapping(appKey = "youtube", actionKey = "getAllPlaylistItems")
    @SuppressWarnings("unchecked")
    public ActionResult getAll(ActionContext context) {
        String playlistId = YouTubeSupport.require(context.configuration(), "playlistId");
        if (playlistId == null) return ActionResult.failure("'playlistId' is required");
        int maxResults = YouTubeSupport.parseIntOpt(context.configuration(), "maxResults", 50);

        try {
            StringBuilder uri = new StringBuilder(BASE + "?part=snippet,contentDetails,status&playlistId=" + playlistId + "&maxResults=" + maxResults);

            String apiKey = YouTubeSupport.resolveApiKey(context);
            if (apiKey != null) uri.append("&key=").append(apiKey);

            Map<String, Object> response = YouTubeSupport.clientBuilder(context).build().get()
                    .uri(uri.toString())
                    .retrieve()
                    .body(Map.class);
            return ActionResult.success(response);
        } catch (Exception e) {
            return ActionResult.failure("YouTube getAllPlaylistItems failed: " + e.getMessage());
        }
    }
}
