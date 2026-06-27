package com.crescendo.apps.spotify;

import com.crescendo.execution.action.ActionContext;
import com.crescendo.execution.action.ActionMapping;
import com.crescendo.utils.RestClient;
import org.springframework.stereotype.Component;

import java.util.Map;

@Component
public class SpotifyPlaylistHandlers {

    private String getAuth(ActionContext context) throws Exception {
        return "Bearer " + context.getCredential("accessToken");
    }

    @ActionMapping(appKey = "spotify", actionKey = "create-playlist")
    public Object createPlaylist(ActionContext context) throws Exception {
        String userId = context.getString("userId"); // Note: often you'd use "me" instead
        String name = context.getString("name");
        return RestClient.builder()
                .url("https://api.spotify.com/v1/users/" + userId + "/playlists")
                .header("Authorization", getAuth(context))
                .header("Content-Type", "application/json")
                .post(Map.of("name", name))
                .execute();
    }

    @ActionMapping(appKey = "spotify", actionKey = "get-playlist")
    public Object getPlaylist(ActionContext context) throws Exception {
        String playlistId = context.getString("playlistId");
        return RestClient.builder()
                .url("https://api.spotify.com/v1/playlists/" + playlistId)
                .header("Authorization", getAuth(context))
                .get()
                .execute();
    }

    @ActionMapping(appKey = "spotify", actionKey = "add-to-playlist")
    public Object addToPlaylist(ActionContext context) throws Exception {
        String playlistId = context.configuration().get("playlistId") != null ? context.configuration().get("playlistId").toString() : "";
        String trackUri = context.configuration().get("trackUri") != null ? context.configuration().get("trackUri").toString() : "";
        
        return RestClient.builder()
                .url("https://api.spotify.com/v1/playlists/" + playlistId + "/tracks")
                .header("Authorization", getAuth(context))
                .header("Content-Type", "application/json")
                .post(Map.of("uris", java.util.List.of(trackUri)))
                .execute();
    }
}
