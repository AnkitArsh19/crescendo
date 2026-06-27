package com.crescendo.apps.youtube;

import com.crescendo.execution.action.ActionContext;
import com.crescendo.execution.action.ActionMapping;
import com.crescendo.execution.action.ActionResult;
import org.springframework.stereotype.Component;

import java.util.Map;

/**
 * Grouped handler for YouTube Playlist operations.
 */
@Component
public class YouTubePlaylistHandlers {

    private static final String BASE = "https://www.googleapis.com/youtube/v3/playlists";

    @ActionMapping(appKey = "youtube", actionKey = "getPlaylist")
    @SuppressWarnings("unchecked")
    public ActionResult get(ActionContext context) {
        String playlistId = YouTubeSupport.require(context.configuration(), "playlistId");
        if (playlistId == null) return ActionResult.failure("'playlistId' is required");

        try {
            String uri = BASE + "?part=snippet,contentDetails,status&id=" + playlistId;
            String apiKey = YouTubeSupport.resolveApiKey(context);
            if (apiKey != null) uri += "&key=" + apiKey;

            Map<String, Object> response = YouTubeSupport.clientBuilder(context).build().get()
                    .uri(uri)
                    .retrieve()
                    .body(Map.class);
            return ActionResult.success(response);
        } catch (Exception e) {
            return ActionResult.failure("YouTube getPlaylist failed: " + e.getMessage());
        }
    }

    @ActionMapping(appKey = "youtube", actionKey = "getAllPlaylists")
    @SuppressWarnings("unchecked")
    public ActionResult getAll(ActionContext context) {
        int maxResults = YouTubeSupport.parseIntOpt(context.configuration(), "maxResults", 50);
        String channelId = YouTubeSupport.opt(context.configuration(), "channelId", null);
        boolean mine = Boolean.parseBoolean(YouTubeSupport.opt(context.configuration(), "mine", "false"));

        try {
            StringBuilder uri = new StringBuilder(BASE + "?part=snippet,contentDetails,status&maxResults=" + maxResults);
            if (channelId != null) uri.append("&channelId=").append(channelId);
            if (mine) uri.append("&mine=true");

            String apiKey = YouTubeSupport.resolveApiKey(context);
            if (apiKey != null) uri.append("&key=").append(apiKey);

            Map<String, Object> response = YouTubeSupport.clientBuilder(context).build().get()
                    .uri(uri.toString())
                    .retrieve()
                    .body(Map.class);
            return ActionResult.success(response);
        } catch (Exception e) {
            return ActionResult.failure("YouTube getAllPlaylists failed: " + e.getMessage());
        }
    }
}
