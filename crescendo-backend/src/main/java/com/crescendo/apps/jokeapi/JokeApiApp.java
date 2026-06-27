package com.crescendo.apps.jokeapi;

import com.crescendo.app.App;
import com.crescendo.apps.AppDefinition;
import com.crescendo.enums.AuthType;
import org.springframework.stereotype.Component;
import java.util.List;
import java.util.Map;

@Component
public class JokeApiApp implements AppDefinition {
    @Override
    public App toApp() {
        return new App("joke-api", "Joke API", """
                JokeAPI is a REST API that serves uniformly and well-formatted jokes. The Crescendo Joke API app is a fun utility to inject humor into your automated notifications.

                **What you can do with Joke API in Crescendo:**
                - Send a "Programming" category joke to your developer team's Slack channel every Friday at 4 PM
                - Reply to a specific trigger phrase in Discord with a random pun
                - Append a clean, family-friendly joke to the footer of a daily newsletter generated in Mailchimp
                - Use placeholder test data while building and debugging a new messaging workflow

                **Actions available:**
                - Get Joke — fetch a random joke, with optional filters for categories (e.g., Programming, Pun, Spooky) and flags to exclude NSFW content

                **Who should use this:** Community managers, internal tooling developers, and anyone looking to boost team morale.

                **Authentication:** None required.
                """,
                "/icons/joke.png", AuthType.NONE,
                List.of(),
                List.of(
                    Map.of("actionKey", "get-joke", "name", "Get Random Joke",
                        "description", "Retrieve a random joke",
                        "configSchema", List.of(
                            Map.of("key", "category", "label", "Category", "type", "select", "required", false,
                                   "options", List.of(
                                       Map.of("value", "Any", "label", "Any"),
                                       Map.of("value", "Programming", "label", "Programming"),
                                       Map.of("value", "Misc", "label", "Miscellaneous"),
                                       Map.of("value", "Dark", "label", "Dark"),
                                       Map.of("value", "Pun", "label", "Pun"),
                                       Map.of("value", "Spooky", "label", "Spooky"),
                                       Map.of("value", "Christmas", "label", "Christmas")
                                   ), "helpText", "Joke category"),
                            Map.of("key", "type", "label", "Type", "type", "select", "required", false,
                                   "options", List.of(
                                       Map.of("value", "", "label", "Any"),
                                       Map.of("value", "single", "label", "Single"),
                                       Map.of("value", "twopart", "label", "Two-Part (setup + delivery)")
                                   ), "helpText", "Joke format"),
                            Map.of("key", "language", "label", "Language", "type", "select", "required", false,
                                   "options", List.of(
                                       Map.of("value", "en", "label", "English"),
                                       Map.of("value", "de", "label", "German"),
                                       Map.of("value", "es", "label", "Spanish"),
                                       Map.of("value", "fr", "label", "French"),
                                       Map.of("value", "pt", "label", "Portuguese")
                                   ), "helpText", "Joke language")))
                )
        ).credentialSchema(List.of()).category("fun").helpUrl("https://v2.jokeapi.dev/");
    }
}
