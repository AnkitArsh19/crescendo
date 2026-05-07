package com.crescendo.apps.googleslides;

import com.crescendo.app.App;
import com.crescendo.apps.AppDefinition;
import com.crescendo.enums.AuthType;
import org.springframework.stereotype.Component;

import java.util.List;
import java.util.Map;

@Component
public class GoogleSlidesApp implements AppDefinition {

    @Override
    public App toApp() {
        return new App("google-slides", "Google Slides", "Create presentations and inspect slide decks",
                "/icons/google-slides.svg", AuthType.OAUTH2,
                List.of(Map.of(
                    "triggerKey", "presentation-updated",
                    "name", "Presentation Updated",
                    "description", "Triggers when a presentation changes",
                    "configSchema", List.of(
                        Map.of("key", "presentationId", "label", "Presentation",
                               "type", "dynamic_dropdown", "resourceType", "presentations",
                               "required", true,
                               "helpText", "Select the presentation to watch")
                    )
                )),
                List.of(
                    Map.of(
                        "actionKey", "create-presentation",
                        "name", "Create Presentation",
                        "description", "Create a new Google Slides presentation",
                        "configSchema", List.of(
                            Map.of("key", "title", "label", "Presentation Title",
                                   "type", "text", "required", true,
                                   "placeholder", "Q1 Review",
                                   "helpText", "Title of the new presentation")
                        )
                    ),
                    Map.of(
                        "actionKey", "get-presentation",
                        "name", "Get Presentation",
                        "description", "Fetch a Google Slides presentation",
                        "configSchema", List.of(
                            Map.of("key", "presentationId", "label", "Presentation",
                                   "type", "dynamic_dropdown", "resourceType", "presentations",
                                   "required", true,
                                   "helpText", "Select the presentation to fetch")
                        )
                    )
                ))
                .credentialSchema(List.of())
                .category("productivity")
                .helpUrl("https://console.cloud.google.com/");
    }
}