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

        return new App("google-slides", "Google Slides", """
                Google Slides is a presentation program included as part of the free, web-based Google Docs Editors suite. The Crescendo Google Slides app allows you to automate the generation and modification of slide decks.

                **What you can do with Google Slides in Crescendo:**
                - Automatically generate a weekly sales presentation using data from Salesforce
                - Replace template placeholder text with client names for automated pitch decks
                - Create a new, blank slide deck whenever a new project is created in Asana
                - Send a Slack notification when a presentation is successfully generated

                **Actions available:**
                - Create Presentation — generate a new blank slideshow
                - Replace Text — find and replace specific placeholder strings across all slides

                **Who should use this:** Sales teams generating pitch decks, educators creating automated lessons, and analysts building recurring visual reports.

                **Authentication:** OAuth 2.0 (connect your Google account).
                """,
                "https://ssl.gstatic.com/images/branding/product/2x/slides_2020q4_48dp.png", AuthType.OAUTH2,
                List.of(
                    Map.of("triggerKey", "presentation-updated", "name", "Presentation Updated",
                        "description", "Triggers when a presentation changes",
                        "configSchema", List.of(presField))
                ),
                List.of(
                    Map.of("actionKey", "create", "name", "Create Presentation",
                        "description", "Create a new Google Slides presentation",
                        "configSchema", List.of(
                            Map.of("key", "title", "label", "Title", "type", "text", "required", true,
                                   "placeholder", "Q1 Review", "helpText", "Presentation title"))),
                    Map.of("actionKey", "get", "name", "Get Presentation",
                        "description", "Fetch a presentation's metadata and slides",
                        "configSchema", List.of(presField)),
                    Map.of("actionKey", "addSlide", "name", "Add Slide",
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