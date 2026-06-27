package com.crescendo.apps.spotify;

import com.crescendo.execution.action.ActionContext;
import com.crescendo.execution.action.ActionMapping;
import com.crescendo.utils.RestClient;
import org.springframework.stereotype.Component;

import java.util.Map;

@Component
public class SpotifyTrackHandlers {

    private String getAuth(ActionContext context) throws Exception {
        return "Bearer " + context.getCredential("accessToken"); // Simplified for brevity
    }

    @ActionMapping(appKey = "spotify", actionKey = "save-track")
    public Object saveTrack(ActionContext context) throws Exception {
        String trackId = context.getString("trackId");
        return RestClient.builder()
                .url("https://api.spotify.com/v1/me/tracks?ids=" + trackId)
                .header("Authorization", getAuth(context))
                .put(Map.of())
                .execute();
    }

    @ActionMapping(appKey = "spotify", actionKey = "search")
    public Object search(ActionContext context) throws Exception {
        String query = context.getString("query");
        String type = (String) context.configuration().getOrDefault("type", "track");
        return RestClient.builder()
                .url("https://api.spotify.com/v1/search?q=" + query + "&type=" + type)
                .header("Authorization", getAuth(context))
                .get()
                .execute();
    }

}
