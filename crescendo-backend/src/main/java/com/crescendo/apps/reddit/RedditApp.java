package com.crescendo.apps.reddit;

import com.crescendo.app.App;
import com.crescendo.apps.AppDefinition;
import com.crescendo.enums.AuthType;
import org.springframework.stereotype.Component;

import java.util.List;
import java.util.Map;

@Component
public class RedditApp implements AppDefinition {

    @Override
    public App toApp() {
        var subredditField = Map.of("key", "subreddit", "label", "Subreddit",
                "type", "dynamic_dropdown", "resourceType", "subreddits",
                "required", true,
                "helpText", "Select a subreddit you're subscribed to");

        return new App("reddit", "Reddit", "Submit posts, add comments, and monitor subreddits",
                "/icons/reddit.svg", AuthType.OAUTH2,

                // ═══ TRIGGERS ═══
                List.of(
                    Map.of(
                        "triggerKey", "new-post",
                        "name", "New Post in Subreddit",
                        "description", "Triggers when a new post appears in a subreddit",
                        "configSchema", List.of(subredditField)
                    ),
                    Map.of(
                        "triggerKey", "new-comment",
                        "name", "New Comment on Post",
                        "description", "Triggers when a new comment is posted in a subreddit",
                        "configSchema", List.of(subredditField)
                    ),
                    Map.of(
                        "triggerKey", "new-hot-post",
                        "name", "New Hot Post",
                        "description", "Triggers when a post reaches Hot in a subreddit",
                        "configSchema", List.of(subredditField)
                    )
                ),

                // ═══ ACTIONS ═══
                List.of(
                    Map.of(
                        "actionKey", "submit-post",
                        "name", "Submit Post",
                        "description", "Submit a new post to a subreddit",
                        "configSchema", List.of(
                            subredditField,
                            Map.of("key", "title", "label", "Post Title",
                                   "type", "text", "required", true,
                                   "placeholder", "Check this out!",
                                   "helpText", "Title of the post"),
                            Map.of("key", "text", "label", "Post Body",
                                   "type", "textarea", "required", false,
                                   "helpText", "Optional body text for a self post"),
                            Map.of("key", "url", "label", "Link URL",
                                   "type", "text", "required", false,
                                   "placeholder", "https://example.com",
                                   "helpText", "Optional URL for a link post (leave empty for text post)")
                        )
                    ),
                    Map.of(
                        "actionKey", "add-comment",
                        "name", "Add Comment",
                        "description", "Add a comment to a post or reply to a comment",
                        "configSchema", List.of(
                            Map.of("key", "thingId", "label", "Post/Comment ID",
                                   "type", "text", "required", true,
                                   "placeholder", "t3_abc123",
                                   "helpText", "The fullname (t3_ for posts, t1_ for comments) to reply to"),
                            Map.of("key", "text", "label", "Comment Body",
                                   "type", "textarea", "required", true,
                                   "helpText", "Comment text in Markdown")
                        )
                    )
                )
        )
        .credentialSchema(List.of())
        .category("communication")
        .helpUrl("https://www.reddit.com/prefs/apps");
    }
}
