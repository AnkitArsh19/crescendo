package com.crescendo.apps.instagram;

import com.crescendo.app.App;
import com.crescendo.apps.AppDefinition;
import com.crescendo.enums.AuthType;
import org.springframework.stereotype.Component;
import java.util.*;

@Component
public class InstagramApp implements AppDefinition {
    public App toApp() {
        return new App(
                "instagram",
                "Instagram Graph API", """
                The Instagram Graph API allows you to programmatically access Instagram Business and Creator accounts. The Crescendo Instagram app enables you to automate your visual social media presence.

                **What you can do with Instagram in Crescendo:**
                - Publish Instagram photos, reels, and stories automatically
                - Reply directly to customer comments and direct messages (DMs)
                - Monitor engagement, follower counts, and post insights in real-time
                - Trigger instant workflows when comments, mentions, or DMs are received

                **Triggers available:**
                - New Comment Received — fires when a user comments on your post
                - New Direct Message — fires when a customer sends a message
                - New Mention — fires when your handle is @mentioned in a caption or comment
                - Instagram Event — generic webhook event from Instagram / Meta

                **Actions available:**
                - Publish Photo Post — one-step publishing for photos and carousels
                - Publish Reel / Video — publish short-form video reels with custom cover
                - Send Direct Message — reply or initiate DMs to Instagram users
                - Reply to Comment — respond to comments on your media posts
                - Get Media Comments — fetch all comments on a post
                - Get Recent Posts — fetch your published photos, reels, and videos
                - Get Media Insights — retrieve views, reach, likes, saves, and shares
                - Get Account Profile — retrieve follower count, bio, and profile stats
                - Create Media Container & Publish Media — 2-step media staging workflow

                **Who should use this:** Social media managers, influencers, and digital marketing teams automating cross-platform content delivery.

                **Authentication:** OAuth 2.0 (connect your linked Facebook / Instagram account).
                """,
                "https://www.google.com/s2/favicons?domain=instagram.com&sz=128",
                AuthType.OAUTH2,
                List.of(
                        Map.of(
                                "triggerKey", "instagram-comment-received",
                                "name", "New Comment Received",
                                "description", "Triggers when someone comments on your Instagram post",
                                "configSchema", List.of(
                                        Map.of("key", "igUserId", "label", "Instagram Account", "type", "dynamic_dropdown", "resourceType", "accounts", "required", false, "helpText", "Optional: Filter for a specific Instagram account")
                                )
                        ),
                        Map.of(
                                "triggerKey", "instagram-message-received",
                                "name", "New Direct Message",
                                "description", "Triggers when a customer sends an Instagram DM",
                                "configSchema", List.of(
                                        Map.of("key", "igUserId", "label", "Instagram Account", "type", "dynamic_dropdown", "resourceType", "accounts", "required", false)
                                )
                        ),
                        Map.of(
                                "triggerKey", "instagram-mention-received",
                                "name", "New Mention",
                                "description", "Triggers when your account is @mentioned in a comment or post caption",
                                "configSchema", List.of()
                        ),
                        Map.of(
                                "triggerKey", "instagram-event",
                                "name", "Instagram Event (Webhook)",
                                "description", "Triggers from any Instagram webhook event payload",
                                "configSchema", List.of()
                        )
                ),
                List.of(
                        Map.of(
                                "actionKey", "publish-photo-post",
                                "name", "Publish Photo Post",
                                "description", "Directly upload and publish a photo to your Instagram feed",
                                "configSchema", List.of(
                                        Map.of("key", "igUserId", "label", "Instagram Account", "type", "dynamic_dropdown", "resourceType", "accounts", "required", true, "helpText", "Select your connected Instagram Business/Creator account"),
                                        Map.of("key", "imageUrl", "label", "Photo (Upload or URL)", "type", "file_or_url", "required", true, "placeholder", "https://example.com/photo.jpg", "helpText", "Upload an image file or provide a public photo URL (JPEG/PNG)"),
                                        Map.of("key", "caption", "label", "Caption & Hashtags", "type", "textarea", "required", false, "placeholder", "Write your post caption, tags, and emojis...")
                                )
                        ),
                        Map.of(
                                "actionKey", "publish-reel-post",
                                "name", "Publish Reel / Video",
                                "description", "Upload and publish a video or Reel to your Instagram feed",
                                "configSchema", List.of(
                                        Map.of("key", "igUserId", "label", "Instagram Account", "type", "dynamic_dropdown", "resourceType", "accounts", "required", true),
                                        Map.of("key", "videoUrl", "label", "Video (Upload or URL)", "type", "file_or_url", "required", true, "placeholder", "https://example.com/reel.mp4", "helpText", "Upload a video file or provide a public MP4/MOV video URL"),
                                        Map.of("key", "caption", "label", "Caption & Hashtags", "type", "textarea", "required", false, "placeholder", "Reel caption..."),
                                        Map.of("key", "coverUrl", "label", "Cover Image (Upload or URL - Optional)", "type", "file_or_url", "required", false, "placeholder", "https://example.com/cover.jpg")
                                )
                        ),
                        Map.of(
                                "actionKey", "send-direct-message",
                                "name", "Send Direct Message (DM)",
                                "description", "Send a direct message reply to an Instagram user",
                                "configSchema", List.of(
                                        Map.of("key", "igUserId", "label", "Instagram Account", "type", "dynamic_dropdown", "resourceType", "accounts", "required", true),
                                        Map.of("key", "recipientId", "label", "Recipient / Recent Contact", "type", "dynamic_dropdown", "resourceType", "recipients", "dependsOn", List.of("igUserId"), "required", true, "helpText", "Select from recent DM contacts or insert dynamic sender ID like {{trigger.sender.id}} (Meta 24-hr window applies)"),
                                        Map.of("key", "message", "label", "Message Text", "type", "textarea", "required", true, "placeholder", "Hello! Thank you for reaching out...")
                                )
                        ),
                        Map.of(
                                "actionKey", "reply-comment",
                                "name", "Reply to Specific Comment",
                                "description", "Post a reply to a specific comment on one of your Instagram posts",
                                "configSchema", List.of(
                                        Map.of("key", "igUserId", "label", "Instagram Account (Optional)", "type", "dynamic_dropdown", "resourceType", "accounts", "required", false, "helpText", "Select your account to browse posts and comments below"),
                                        Map.of("key", "mediaId", "label", "Instagram Post / Reel (Optional)", "type", "dynamic_dropdown", "resourceType", "media", "dependsOn", List.of("igUserId"), "required", false, "helpText", "Select a post to browse and pick its comments below"),
                                        Map.of("key", "commentId", "label", "Comment to Reply To", "type", "dynamic_dropdown", "resourceType", "comments", "dependsOn", List.of("mediaId"), "required", true, "helpText", "Select a comment from the dropdown, or type a Comment ID / insert a trigger variable like {{trigger.commentId}}"),
                                        Map.of("key", "message", "label", "Reply Message", "type", "textarea", "required", true, "placeholder", "Thanks for your feedback!")
                                )
                        ),
                        Map.of(
                                "actionKey", "reply-latest-comment",
                                "name", "Reply to Latest Comment on Post",
                                "description", "Automatically find and reply to the newest comment on a specific post, with optional keyword filtering",
                                "configSchema", List.of(
                                        Map.of("key", "igUserId", "label", "Instagram Account", "type", "dynamic_dropdown", "resourceType", "accounts", "required", true, "helpText", "Select your connected Instagram account"),
                                        Map.of("key", "mediaId", "label", "Instagram Post / Reel", "type", "dynamic_dropdown", "resourceType", "media", "dependsOn", List.of("igUserId"), "required", true, "helpText", "Select the post whose latest comment to reply to"),
                                        Map.of("key", "message", "label", "Reply Message", "type", "textarea", "required", true, "placeholder", "Thanks for commenting! Check your DMs for the link."),
                                        Map.of("key", "matchingKeyword", "label", "Filter Keyword (Optional)", "type", "text", "required", false, "placeholder", "e.g. price, link, info, yes", "helpText", "Optional: Only reply if the comment contains this word (case-insensitive). If left blank, replies to the latest comment.")
                                )
                        ),
                        Map.of(
                                "actionKey", "get-media-comments",
                                "name", "Get Media Comments",
                                "description", "List recent comments on a specific post",
                                "configSchema", List.of(
                                        Map.of("key", "igUserId", "label", "Instagram Account", "type", "dynamic_dropdown", "resourceType", "accounts", "required", true),
                                        Map.of("key", "mediaId", "label", "Instagram Post / Reel", "type", "dynamic_dropdown", "resourceType", "media", "dependsOn", List.of("igUserId"), "required", true, "helpText", "Select a recent post from your account"),
                                        Map.of("key", "limit", "label", "Limit", "type", "text", "required", false, "placeholder", "25")
                                )
                        ),
                        Map.of(
                                "actionKey", "get-user-media",
                                "name", "Get Recent Posts",
                                "description", "Fetch published photos, reels, and videos from your account",
                                "configSchema", List.of(
                                        Map.of("key", "igUserId", "label", "Instagram Account", "type", "dynamic_dropdown", "resourceType", "accounts", "required", true),
                                        Map.of("key", "limit", "label", "Limit", "type", "text", "required", false, "placeholder", "25")
                                )
                        ),
                        Map.of(
                                "actionKey", "get-media-insights",
                                "name", "Get Media Insights",
                                "description", "Retrieve reach, impressions, likes, saves, and shares for a post",
                                "configSchema", List.of(
                                        Map.of("key", "igUserId", "label", "Instagram Account", "type", "dynamic_dropdown", "resourceType", "accounts", "required", true),
                                        Map.of("key", "mediaId", "label", "Instagram Post / Reel", "type", "dynamic_dropdown", "resourceType", "media", "dependsOn", List.of("igUserId"), "required", true, "helpText", "Select a recent post from your account")
                                )
                        ),
                        Map.of(
                                "actionKey", "get-account-info",
                                "name", "Get Account Profile",
                                "description", "Retrieve account statistics, follower count, and bio",
                                "configSchema", List.of(
                                        Map.of("key", "igUserId", "label", "Instagram Account", "type", "dynamic_dropdown", "resourceType", "accounts", "required", true)
                                )
                        ),
                        Map.of(
                                "actionKey", "create-media-container",
                                "name", "Create Media Container (Staging)",
                                "description", "Create a staged image or video container for advanced multi-step workflows",
                                "configSchema", List.of(
                                        Map.of("key", "igUserId", "label", "Instagram Account", "type", "dynamic_dropdown", "resourceType", "accounts", "required", true),
                                        Map.of("key", "imageUrl", "label", "Photo (Upload or URL)", "type", "file_or_url", "required", true, "placeholder", "https://example.com/photo.jpg"),
                                        Map.of("key", "caption", "label", "Caption", "type", "textarea", "required", false, "placeholder", "Enter post caption, hashtags...")
                                )
                        ),
                        Map.of(
                                "actionKey", "publish-media",
                                "name", "Publish Media Container",
                                "description", "Publish a previously staged media container",
                                "configSchema", List.of(
                                        Map.of("key", "igUserId", "label", "Instagram Account", "type", "dynamic_dropdown", "resourceType", "accounts", "required", true),
                                        Map.of("key", "creationId", "label", "Creation ID", "type", "text", "required", true, "placeholder", "Media container ID from previous step")
                                )
                        )
                )
        ).credentialSchema(List.of(
                Map.of("key", "accessToken", "label", "Access Token", "type", "password", "required", true),
                Map.of("key", "graphVersion", "label", "Graph Version", "type", "text", "required", false, "placeholder", "v26.0")
        )).altAuthType(AuthType.OAUTH2).category("social").helpUrl("https://developers.facebook.com/docs/instagram-platform/");
    }
}
