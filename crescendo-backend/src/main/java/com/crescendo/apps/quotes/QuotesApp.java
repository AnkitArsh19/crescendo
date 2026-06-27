package com.crescendo.apps.quotes;

import com.crescendo.app.App;
import com.crescendo.apps.AppDefinition;
import com.crescendo.enums.AuthType;
import org.springframework.stereotype.Component;
import java.util.List;
import java.util.Map;

@Component
public class QuotesApp implements AppDefinition {
    @Override
    public App toApp() {
        return new App("quotes", "Random Quotes", """
                The Random Quotes app is a lightweight utility that connects to public quote APIs to fetch inspirational, funny, or thought-provoking sayings from historical figures and authors.

                **What you can do with Quotes in Crescendo:**
                - Post an inspirational "Quote of the Day" to a company-wide Slack channel every morning
                - Append a random thought-provoking quote to the footer of internal team newsletters
                - Use placeholder quote data while testing new messaging workflows and formatting
                - Send a motivational SMS to yourself via Twilio before a big presentation

                **Actions available:**
                - Get Quote — fetch a random quote with its author

                **Who should use this:** Community managers and developers looking to add a touch of personality to their automated messaging.

                **Authentication:** None required.
                """,
                "/icons/quotes.svg", AuthType.NONE,
                List.of(),
                List.of(
                    Map.of("actionKey", "get-random-quote", "name", "Get Random Quote",
                        "description", "Retrieve a random inspirational quote",
                        "configSchema", List.of(
                            Map.of("key", "author", "label", "Author Filter", "type", "text", "required", false,
                                   "placeholder", "Albert Einstein", "helpText", "Filter by specific author"))),
                    Map.of("actionKey", "get-by-category", "name", "Get Quote by Category",
                        "description", "Get a quote from a specific category",
                        "configSchema", List.of(
                            Map.of("key", "category", "label", "Category", "type", "select", "required", false,
                                   "options", List.of(
                                       Map.of("value", "", "label", "Any"),
                                       Map.of("value", "inspirational", "label", "Inspirational"),
                                       Map.of("value", "life", "label", "Life"),
                                       Map.of("value", "love", "label", "Love"),
                                       Map.of("value", "humor", "label", "Humor"),
                                       Map.of("value", "wisdom", "label", "Wisdom"),
                                       Map.of("value", "motivational", "label", "Motivational"),
                                       Map.of("value", "success", "label", "Success"),
                                       Map.of("value", "technology", "label", "Technology")
                                   ), "helpText", "Quote category"))),
                    Map.of("actionKey", "get-quote-of-day", "name", "Quote of the Day",
                        "description", "Get the daily featured quote",
                        "configSchema", List.of())
                )
        ).credentialSchema(List.of()).category("fun").helpUrl("");
    }
}
