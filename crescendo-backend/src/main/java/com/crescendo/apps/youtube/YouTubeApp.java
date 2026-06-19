package com.crescendo.apps.youtube;

import com.crescendo.app.App;
import com.crescendo.apps.AppDefinition;
import com.crescendo.enums.AuthType;
import org.springframework.stereotype.Component;

import java.util.List;
import java.util.Map;

@Component
public class YouTubeApp implements AppDefinition {
    @Override
    public App toApp() {
        return new App("youtube", "YouTube", "Search, list, upload, and update YouTube videos",
                "https://www.google.com/s2/favicons?domain=youtube.com&sz=128", AuthType.OAUTH2,
                List.of(),
                List.of(
                        Map.of("actionKey", "search", "name", "Search Videos", "description", "Search YouTube videos",
                                "configSchema", List.of(
                                        Map.of("key", "query", "label", "Query", "type", "text", "required", true),
                                        Map.of("key", "maxResults", "label", "Max Results", "type", "text", "required", false, "placeholder", "10"))),
                        Map.of("actionKey", "list-my-videos", "name", "List My Videos", "description", "List videos from the authenticated channel uploads playlist",
                                "configSchema", List.of(Map.of("key", "maxResults", "label", "Max Results", "type", "text", "required", false, "placeholder", "10"))),
                        Map.of("actionKey", "upload-video", "name", "Upload Video", "description", "Upload a Base64 encoded video",
                                "configSchema", List.of(
                                        Map.of("key", "title", "label", "Title", "type", "text", "required", true),
                                        Map.of("key", "description", "label", "Description", "type", "textarea", "required", false),
                                        Map.of("key", "privacyStatus", "label", "Privacy", "type", "select", "required", false,
                                                "options", List.of(Map.of("value", "private", "label", "Private"), Map.of("value", "unlisted", "label", "Unlisted"), Map.of("value", "public", "label", "Public"))),
                                        Map.of("key", "tags", "label", "Tags CSV", "type", "text", "required", false),
                                        Map.of("key", "categoryId", "label", "Category ID", "type", "text", "required", false, "placeholder", "22"),
                                        Map.of("key", "videoBase64", "label", "Video Base64", "type", "textarea", "required", true),
                                        Map.of("key", "mimeType", "label", "MIME Type", "type", "text", "required", false, "placeholder", "video/mp4"))),
                        Map.of("actionKey", "update-video", "name", "Update Video Metadata", "description", "Update title, description, tags, category, or privacy",
                                "configSchema", List.of(
                                        Map.of("key", "videoId", "label", "Video ID", "type", "text", "required", true),
                                        Map.of("key", "title", "label", "Title", "type", "text", "required", true),
                                        Map.of("key", "description", "label", "Description", "type", "textarea", "required", false),
                                        Map.of("key", "privacyStatus", "label", "Privacy", "type", "select", "required", false,
                                                "options", List.of(Map.of("value", "private", "label", "Private"), Map.of("value", "unlisted", "label", "Unlisted"), Map.of("value", "public", "label", "Public"))),
                                        Map.of("key", "tags", "label", "Tags CSV", "type", "text", "required", false),
                                        Map.of("key", "categoryId", "label", "Category ID", "type", "text", "required", false, "placeholder", "22")))
                )
        ).credentialSchema(List.of(
                        Map.of("key", "accessToken", "label", "OAuth Access Token", "type", "password", "required", false),
                        Map.of("key", "apiKey", "label", "API Key", "type", "password", "required", false)))
                .category("social")
                .helpUrl("https://developers.google.com/youtube/v3");
    }
}
