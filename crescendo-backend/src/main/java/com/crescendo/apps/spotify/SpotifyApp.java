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
        return new App("spotify", "Spotify", """
                Spotify is the world's most popular audio streaming subscription service. The Crescendo Spotify app lets you search for music, manage your playlists, and control your library automatically.

                **What you can do with Spotify in Crescendo:**
                - Search for tracks, albums, and artists based on external data
                - Automatically add songs from Discord/Slack messages to a collaborative playlist
                - Create "Song of the Day" automated posts
                - Back up your saved tracks to a Google Sheet

                **Actions available:**
                - Search Spotify — find music using queries and filters
                - Add Track to Playlist — add a specific song to a playlist
                - Create Playlist — generate a new playlist dynamically
                - Save Track — add a song to your Liked Songs

                **Who should use this:** Music enthusiasts, community managers building collaborative playlists, and users wanting to automate their music libraries.

                **Authentication:** OAuth 2.0 (connect your Spotify account) or API Key.
                """,
                "https://www.google.com/s2/favicons?domain=spotify.com&sz=128", AuthType.OAUTH2,
                List.of(
                    Map.of("triggerKey", "track-saved", "name", "New Saved Track",
                        "description", "Triggers when a track is saved to your library",
                        "configSchema", List.of()),
                    Map.of("triggerKey", "new-playlist-track", "name", "New Playlist Track",
                        "description", "Triggers when a track is added to a playlist",
                        "configSchema", List.of(
                            Map.of("key", "playlistId", "label", "Playlist", "type", "dynamic_dropdown",
                                   "resourceType", "playlists", "required", true, "helpText", "Playlist to watch")))
                ),
                List.of(
                    Map.of("actionKey", "search", "name", "Search Spotify",
                        "description", "Search for tracks, albums, artists, or playlists",
                        "configSchema", List.of(
                            Map.of("key", "query", "label", "Search Query", "type", "text", "required", true,
                                   "helpText", "e.g. 'Bohemian Rhapsody'"),
                            Map.of("key", "type", "label", "Type", "type", "dropdown", "required", false,
                                   "options", List.of(
                                       Map.of("value", "track", "label", "Track"),
                                       Map.of("value", "album", "label", "Album"),
                                       Map.of("value", "artist", "label", "Artist"),
                                       Map.of("value", "playlist", "label", "Playlist")
                                   ), "helpText", "Content type"),
                            Map.of("key", "limit", "label", "Max Results", "type", "text", "required", false,
                                   "placeholder", "10", "helpText", "Results to return (max 50)"))),
                    Map.of("actionKey", "add-to-playlist", "name", "Add Track to Playlist",
                        "description", "Add a track to a playlist",
                        "configSchema", List.of(
                            Map.of("key", "playlistId", "label", "Playlist", "type", "dynamic_dropdown",
                                   "resourceType", "playlists", "required", true, "helpText", "Target playlist"),
                            Map.of("key", "trackUri", "label", "Track URI", "type", "text", "required", true,
                                   "placeholder", "spotify:track:4iV5W9uYEdYUVa79Axb7Rh", "helpText", "Spotify track URI or URL"))),
                    Map.of("actionKey", "create-playlist", "name", "Create Playlist",
                        "description", "Create a new Spotify playlist",
                        "configSchema", List.of(
                            Map.of("key", "name", "label", "Playlist Name", "type", "text", "required", true, "helpText", "Name"),
                            Map.of("key", "description", "label", "Description", "type", "textarea", "required", false, "helpText", "Playlist description"),
                            Map.of("key", "isPublic", "label", "Public?", "type", "dropdown", "required", false,
                                   "options", List.of(Map.of("value","true","label","Yes"), Map.of("value","false","label","No")),
                                   "helpText", "Make playlist public"))),
                    Map.of("actionKey", "save-track", "name", "Save Track",
                        "description", "Save a track to your library (Like)",
                        "configSchema", List.of(
                            Map.of("key", "trackId", "label", "Track ID/URI", "type", "text", "required", true,
                                   "placeholder", "spotify:track:4iV5W9uYEdYUVa79Axb7Rh", "helpText", "Track ID, URI, or URL"))),
                    Map.of("actionKey", "get-playlist", "name", "Get Playlist",
                        "description", "Get details of a Spotify playlist",
                        "configSchema", List.of(
                            Map.of("key", "playlistId", "label", "Playlist URL or ID", "type", "text", "required", true,
                                   "placeholder", "37i9dQZF1DXcBWIGoYBM5M", "helpText", "Playlist URL or ID")))
                ))
                .altAuthType(AuthType.APIKEY)
                .credentialSchema(List.of(
                    Map.of("key", "clientId", "label", "Client ID", "type", "text", "required", true,
                           "placeholder", "e.g. 140396bc...", "helpText", "Spotify Developer App Client ID", "authType", "APIKEY"),
                    Map.of("key", "clientSecret", "label", "Client Secret", "type", "password", "required", true,
                           "placeholder", "e.g. 03e44618...", "helpText", "Spotify Developer App Client Secret", "authType", "APIKEY")
                )).category("fun").helpUrl("https://developer.spotify.com/dashboard");
    }
}
