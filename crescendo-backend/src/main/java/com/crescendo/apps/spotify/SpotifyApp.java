package com.crescendo.apps.spotify;

import com.crescendo.app.App;
import com.crescendo.apps.AppDefinition;
import com.crescendo.enums.AuthType;
import org.springframework.stereotype.Component;

import java.util.List;
import java.util.Map;

@Component
public class SpotifyApp implements AppDefinition {

    @Override
    public App toApp() {
        return new App("spotify", "Spotify", "Control playback and manage playlists",
                "/icons/spotify.svg", AuthType.OAUTH2,
                List.of(Map.of(
                    "triggerKey", "track-saved",
                    "name", "Track Saved",
                    "description", "Triggers when a track is saved to the user's library"
                )),
                List.of(
                    Map.of(
                        "actionKey", "search",
                        "name", "Search Spotify",
                        "description", "Search for tracks, albums, artists, or playlists",
                        "configSchema", List.of(
                            Map.of("key", "query", "label", "Search Query",
                                   "type", "text", "required", true,
                                   "helpText", "e.g. 'Bohemian Rhapsody', 'Taylor Swift', 'lo-fi beats'"),
                            Map.of("key", "type", "label", "Search Type",
                                   "type", "dropdown", "required", false,
                                   "options", List.of(
                                       Map.of("value", "track", "label", "Track"),
                                       Map.of("value", "album", "label", "Album"),
                                       Map.of("value", "artist", "label", "Artist"),
                                       Map.of("value", "playlist", "label", "Playlist")
                                   ),
                                   "helpText", "Type of content to search for (default: track)"),
                            Map.of("key", "limit", "label", "Max Results",
                                   "type", "number", "required", false,
                                   "helpText", "Number of results to return (default: 10, max: 50)")
                        )
                    ),
                    Map.of(
                        "actionKey", "get-playlist",
                        "name", "Get Playlist",
                        "description", "Get details of a Spotify playlist",
                        "configSchema", List.of(
                            Map.of("key", "playlistId", "label", "Playlist URL or ID",
                                   "type", "text", "required", true,
                                   "placeholder", "https://open.spotify.com/playlist/37i9dQZF1DXcBWIGoYBM5M",
                                   "helpText", "Paste a Spotify playlist URL or just the playlist ID")
                        )
                    ),
                    Map.of(
                        "actionKey", "add-to-playlist",
                        "name", "Add Track to Playlist",
                        "description", "Add a track to a Spotify playlist (requires OAuth connection)",
                        "configSchema", List.of(
                            Map.of("key", "playlistId", "label", "Playlist",
                                   "type", "dynamic_dropdown", "resourceType", "playlists",
                                   "required", true,
                                   "helpText", "Select the playlist to add the track to"),
                            Map.of("key", "trackUri", "label", "Track",
                                   "type", "dynamic_dropdown", "resourceType", "tracks",
                                   "required", true, "dependsOn", "playlistId",
                                   "helpText", "Select the track to add (or type a Spotify URI)")
                        )
                    )
                ))
                /*
                 * Dual auth: OAuth2 (admin) + APIKEY (normal users).
                 *
                 * Admin users connect via the normal OAuth2 flow using server-side
                 * credentials in application.properties.
                 *
                 * Normal users provide their own Spotify Developer App credentials
                 * (Client ID + Client Secret) which are used via the Client Credentials flow.
                 */
                .altAuthType(AuthType.APIKEY)
                .credentialSchema(List.of(
                    Map.of(
                        "key", "clientId",
                        "label", "Client ID",
                        "type", "text",
                        "required", true,
                        "placeholder", "e.g. 140396bc11e84b3c...",
                        "helpText", "Your Spotify Developer App Client ID",
                        "authType", "APIKEY"
                    ),
                    Map.of(
                        "key", "clientSecret",
                        "label", "Client Secret",
                        "type", "password",
                        "required", true,
                        "placeholder", "e.g. 03e446184d284227...",
                        "helpText", "Your Spotify Developer App Client Secret",
                        "authType", "APIKEY"
                    )
                ))
                .category("fun")
                .helpUrl("https://developer.spotify.com/dashboard");
    }
}
