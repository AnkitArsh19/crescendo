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
        var presField = Map.of("key", "presentationId", "label", "Presentation",
                "type", "dynamic_dropdown", "resourceType", "presentations",
                "required", true, "helpText", "Select the presentation");

        return new App("google-slides", "Google Slides", "Create presentations and manage slide decks",
                "https://ssl.gstatic.com/images/branding/product/2x/slides_2020q4_48dp.png", AuthType.OAUTH2,
                List.of(
                    Map.of("triggerKey", "presentation-updated", "name", "Presentation Updated",
                        "description", "Triggers when a presentation changes",
                        "configSchema", List.of(presField))
                ),
                List.of(
                    Map.of("actionKey", "create-presentation", "name", "Create Presentation",
                        "description", "Create a new Google Slides presentation",
                        "configSchema", List.of(
                            Map.of("key", "title", "label", "Title", "type", "text", "required", true,
                                   "placeholder", "Q1 Review", "helpText", "Presentation title"))),
                    Map.of("actionKey", "get-presentation", "name", "Get Presentation",
                        "description", "Fetch a presentation's metadata and slides",
                        "configSchema", List.of(presField)),
                    Map.of("actionKey", "add-slide", "name", "Add Slide",
                        "description", "Add a new slide to a presentation",
                        "configSchema", List.of(presField,
                            Map.of("key", "layout", "label", "Layout", "type", "select", "required", false,
                                   "options", List.of(
                                       Map.of("value", "BLANK", "label", "Blank"),
                                       Map.of("value", "TITLE", "label", "Title Slide"),
                                       Map.of("value", "TITLE_AND_BODY", "label", "Title and Body"),
                                       Map.of("value", "SECTION_HEADER", "label", "Section Header")
                                   ), "helpText", "Slide layout (default: Blank)")))
                ))
                .credentialSchema(List.of())
                .category("productivity")
                .helpUrl("https://console.cloud.google.com/");
    }
}