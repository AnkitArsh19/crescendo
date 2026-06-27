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
        return new App("youtube", "YouTube", """
                YouTube is a global online video sharing and social media platform. The Crescendo YouTube app allows you to automate video publishing, metadata updates, and search.

                **What you can do with YouTube in Crescendo:**
                - Upload a daily Zoom meeting recording automatically and set it as an "Unlisted" video
                - Monitor YouTube for specific keywords and alert your team in Discord when a relevant video is found
                - Bulk update the tags and descriptions of your recent uploads using data stored in a Google Sheet
                - Generate a weekly RSS-style email digest of the latest videos published to your channel

                **Actions available:**
                - Search Videos — find videos matching a specific query
                - List My Videos — retrieve the latest uploads from the authenticated channel
                - Upload Video — post a new video file (Base64) with a title, description, and privacy settings
                - Update Video Metadata — change the title, tags, or category of an existing upload

                **Who should use this:** Video creators, media agencies, and marketing teams managing a high-volume content pipeline.

                **Authentication:** OAuth 2.0 (connect your Google account) or an API Key.
                """,
                "https://www.google.com/s2/favicons?domain=youtube.com&sz=128", AuthType.OAUTH2,
                List.of(),
                List.of(
                        Map.of("actionKey", "search", "name", "Search Videos", "description", "Search YouTube videos",
                                "configSchema", List.of(
                                        Map.of("key", "query", "label", "Query", "type", "text", "required", true),
                                        Map.of("key", "maxResults", "label", "Max Results", "type", "text", "required", false, "placeholder", "10"),
                                        Map.of("key", "channelId", "label", "Channel ID", "type", "text", "required", false),
                                        Map.of("key", "forDeveloper", "label", "For Developer", "type", "select", "required", false,
                                                "options", List.of(Map.of("value", "true", "label", "True"), Map.of("value", "false", "label", "False"))),
                                        Map.of("key", "publishedAfter", "label", "Published After", "type", "text", "required", false, "placeholder", "RFC 3339 format"),
                                        Map.of("key", "publishedBefore", "label", "Published Before", "type", "text", "required", false, "placeholder", "RFC 3339 format"),
                                        Map.of("key", "regionCode", "label", "Region Code", "type", "text", "required", false, "placeholder", "US"),
                                        Map.of("key", "relatedToVideoId", "label", "Related To Video ID", "type", "text", "required", false),
                                        Map.of("key", "videoCategoryId", "label", "Video Category ID", "type", "text", "required", false),
                                        Map.of("key", "videoSyndicated", "label", "Video Syndicated", "type", "select", "required", false,
                                                "options", List.of(Map.of("value", "true", "label", "True"), Map.of("value", "false", "label", "False"))),
                                        Map.of("key", "videoType", "label", "Video Type", "type", "select", "required", false,
                                                "options", List.of(Map.of("value", "any", "label", "Any"), Map.of("value", "episode", "label", "Episode"), Map.of("value", "movie", "label", "Movie"))),
                                        Map.of("key", "order", "label", "Order", "type", "select", "required", false,
                                                "options", List.of(Map.of("value", "date", "label", "Date"), Map.of("value", "relevance", "label", "Relevance"), Map.of("value", "rating", "label", "Rating"), Map.of("value", "title", "label", "Title"), Map.of("value", "viewCount", "label", "View Count"))),
                                        Map.of("key", "safeSearch", "label", "Safe Search", "type", "select", "required", false,
                                                "options", List.of(Map.of("value", "moderate", "label", "Moderate"), Map.of("value", "none", "label", "None"), Map.of("value", "strict", "label", "Strict"))))),
                        Map.of("actionKey", "getAllVideos", "name", "List My Videos", "description", "List videos from the authenticated channel uploads playlist",
                                "configSchema", List.of(Map.of("key", "maxResults", "label", "Max Results", "type", "text", "required", false, "placeholder", "10"))),
                        Map.of("actionKey", "uploadVideo", "name", "Upload Video", "description", "Upload a Base64 encoded video",
                                "configSchema", List.of(
                                        Map.of("key", "title", "label", "Title", "type", "text", "required", true),
                                        Map.of("key", "description", "label", "Description", "type", "textarea", "required", false),
                                        Map.of("key", "privacyStatus", "label", "Privacy", "type", "select", "required", false,
                                                "options", List.of(Map.of("value", "private", "label", "Private"), Map.of("value", "unlisted", "label", "Unlisted"), Map.of("value", "public", "label", "Public"))),
                                        Map.of("key", "tags", "label", "Tags CSV", "type", "text", "required", false),
                                        Map.of("key", "categoryId", "label", "Category ID", "type", "text", "required", false, "placeholder", "22"),
                                        Map.of("key", "videoBase64", "label", "Video Base64", "type", "textarea", "required", true),
                                        Map.of("key", "mimeType", "label", "MIME Type", "type", "text", "required", false, "placeholder", "video/mp4"))),
                        Map.of("actionKey", "updateVideo", "name", "Update Video Metadata", "description", "Update title, description, tags, category, or privacy",
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
