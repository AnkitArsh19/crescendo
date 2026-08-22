package com.crescendo.apps.spotify;

import com.crescendo.execution.action.ActionContext;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

import java.util.Map;
import java.util.UUID;

import static org.junit.jupiter.api.Assertions.*;

class SpotifyTrackHandlersTest {

    private final SpotifySupport spotifySupport = new SpotifySupport();
    private final SpotifyTrackHandlers handlers = new SpotifyTrackHandlers(spotifySupport);

    private ActionContext createContext(Map<String, Object> config, Map<String, Object> credentials) {
        return new ActionContext("spotify", "save-track", config, credentials, Map.of(), UUID.randomUUID(), UUID.randomUUID(), UUID.randomUUID(), 1);
    }

    @Test
    @DisplayName("resolveTrackId extracts raw 22-char Spotify ID")
    void resolveTrackId_fromRawId() {
        String rawId = "4cOdK2wGLETKBW3PvgPWqT";
        String resolved = handlers.resolveTrackId(rawId, "Bearer dummy");
        assertEquals(rawId, resolved);
    }

    @Test
    @DisplayName("resolveTrackId extracts track ID from full Spotify URL")
    void resolveTrackId_fromSpotifyUrl() {
        String url = "https://open.spotify.com/track/4cOdK2wGLETKBW3PvgPWqT?si=123456789";
        String resolved = handlers.resolveTrackId(url, "Bearer dummy");
        assertEquals("4cOdK2wGLETKBW3PvgPWqT", resolved);
    }

    @Test
    @DisplayName("resolveTrackId extracts track ID from Spotify URI")
    void resolveTrackId_fromSpotifyUri() {
        String uri = "spotify:track:4cOdK2wGLETKBW3PvgPWqT";
        String resolved = handlers.resolveTrackId(uri, "Bearer dummy");
        assertEquals("4cOdK2wGLETKBW3PvgPWqT", resolved);
    }

    @Test
    @DisplayName("saveTrack throws exception when track input is missing")
    void saveTrack_throwsWhenTrackMissing() {
        ActionContext context = createContext(Map.of(), Map.of("accessToken", "valid-token"));
        assertThrows(IllegalArgumentException.class, () -> handlers.saveTrack(context));
    }

    @Test
    @DisplayName("saveTrack rejects API-key only connections with clear guidance to use OAuth")
    void saveTrack_rejectsApiKeyOnlyConnection() {
        Map<String, Object> config = Map.of("trackId", "4cOdK2wGLETKBW3PvgPWqT");
        Map<String, Object> apiKeyCreds = Map.of("clientId", "id123", "clientSecret", "sec123");
        ActionContext context = createContext(config, apiKeyCreds);

        IllegalArgumentException ex = assertThrows(IllegalArgumentException.class, () -> handlers.saveTrack(context));
        assertTrue(ex.getMessage().contains("Connect with OAuth"));
    }

    @Test
    @DisplayName("search throws exception when query is missing")
    void search_throwsWhenQueryMissing() {
        ActionContext context = new ActionContext("spotify", "search", Map.of(), Map.of("accessToken", "valid-token"), Map.of(), UUID.randomUUID(), UUID.randomUUID(), UUID.randomUUID(), 1);
        assertThrows(IllegalArgumentException.class, () -> handlers.search(context));
    }
}
