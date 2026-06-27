package com.crescendo.apps.medium;

import com.crescendo.app.App;
import com.crescendo.apps.AppDefinition;
import com.crescendo.enums.AuthType;
import org.springframework.stereotype.Component;
import java.util.*;

@Component
public class MediumApp implements AppDefinition {
    public App toApp() {
        return new App(
                "medium",
                "Medium", """
                Medium is an open platform where readers find dynamic thinking, and where expert and undiscovered voices can share their writing on any topic. The Crescendo Medium app automates content publishing.

                **What you can do with Medium in Crescendo:**
                - Automatically publish a drafted Notion page to Medium once its status changes to "Ready"
                - Generate a weekly AI-written industry summary using Gemini and publish it straight to your profile
                - Cross-post articles from your personal WordPress blog onto Medium for wider reach
                - Schedule posts in advance by combining this app with the Schedule trigger

                **Actions available:**
                - Create Post — publish a new article with a Title, Content (Markdown/HTML), and Publish Status (Draft/Public)

                **Who should use this:** Content creators, marketers, and developers looking to streamline their publishing pipeline.

                **Authentication:** Integration Token (generate in your Medium account settings).
                """,
                "https://www.google.com/s2/favicons?domain=medium.com&sz=128",
                AuthType.APIKEY,
                List.of(),
                List.of(
                        Map.of(
                                "actionKey", "createPost",
                                "name", "Create Post",
                                "description", "Publish a Medium post",
                                "configSchema", List.of(
                                        Map.of("key", "title", "label", "Title", "type", "text", "required", true),
                                        Map.of("key", "content", "label", "Content", "type", "textarea", "required", true),
                                        Map.of(
                                                "key", "contentFormat",
                                                "label", "Content Format",
                                                "type", "select",
                                                "required", false,
                                                "options", List.of(
                                                        Map.of("value", "markdown", "label", "Markdown"),
                                                        Map.of("value", "html", "label", "HTML")
                                                )
                                        ),
                                        Map.of(
                                                "key", "publishStatus",
                                                "label", "Publish Status",
                                                "type", "select",
                                                "required", false,
                                                "options", List.of(
                                                        Map.of("value", "draft", "label", "Draft"),
                                                        Map.of("value", "public", "label", "Public")
                                                )
                                        )
                                )
                        )
                )
        ).credentialSchema(List.of(
                Map.of("key", "integrationToken", "label", "Integration Token", "type", "password", "required", true)
        )).category("social").helpUrl("https://github.com/Medium/medium-api-docs");
    }
}
