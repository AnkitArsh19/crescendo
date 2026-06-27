package com.crescendo.apps.catfacts;

import com.crescendo.app.App;
import com.crescendo.apps.AppDefinition;
import com.crescendo.enums.AuthType;
import org.springframework.stereotype.Component;
import java.util.List;
import java.util.Map;

@Component
public class CatFactsApp implements AppDefinition {
    @Override
    public App toApp() {
        return new App("cat-facts", "Cat Facts", """
                The Cat Facts app is a fun, lightweight utility that connects to the popular public Cat Facts API, retrieving random trivia about felines.

                **What you can do with Cat Facts in Crescendo:**
                - Send a random cat fact to a Discord channel every morning at 9 AM
                - Append a cat fact to the footer of internal team emails
                - Reply to specific Twitter mentions with a fun piece of trivia
                - Test out new webhook or message formatting configurations using predictable dummy data

                **Actions available:**
                - Get Fact — returns a single, random string containing a cat fact

                **Who should use this:** Community managers looking to add engagement, and developers testing new messaging workflows.

                **Authentication:** None required.
                """,
                "/icons/catfacts.svg", AuthType.NONE,
                List.of(),
                List.of(
                    Map.of("actionKey", "get-fact", "name", "Get Random Cat Fact",
                        "description", "Retrieve a single random cat fact",
                        "configSchema", List.of()),
                    Map.of("actionKey", "get-multiple-facts", "name", "Get Multiple Facts",
                        "description", "Retrieve multiple cat facts at once",
                        "configSchema", List.of(
                            Map.of("key", "limit", "label", "Number of Facts", "type", "text", "required", false,
                                   "placeholder", "5", "helpText", "How many facts (default: 5)")))
                )
        ).credentialSchema(List.of()).category("fun").helpUrl("https://catfact.ninja/");
    }
}
