package com.crescendo.apps.html;

import com.crescendo.app.App;
import com.crescendo.apps.AppDefinition;
import com.crescendo.enums.AuthType;
import org.springframework.stereotype.Component;

import java.util.List;
import java.util.Map;

/**
 * AppDefinition for HTML.
 */
@Component
public class HtmlApp implements AppDefinition {

    @Override
    public App toApp() {
        return new App(
                "html",
                "HTML",
                """
                Work with HTML.
                
                This integration provides operations for:
                - **Generate HTML Template**: Generate an HTML template
                - **Extract HTML Content**: Extract data from an HTML string or binary
                - **Convert to HTML Table**: Convert a JSON array to an HTML table
                """,
                "https://www.google.com/s2/favicons?domain=html.com&sz=128", // Generic icon
                AuthType.NONE,
                List.of(),
                List.of(
                        Map.of(
                                "actionKey", "html:generateHtmlTemplate",
                                "name", "Generate HTML Template",
                                "description", "Generate HTML template",
                                "configSchema", List.of(
                                        Map.of("key", "html", "label", "HTML Template", "type", "text", "required", true)
                                )
                        ),
                        Map.of(
                                "actionKey", "html:extractHtmlContent",
                                "name", "Extract HTML Content",
                                "description", "Extract HTML Content",
                                "configSchema", List.of(
                                        Map.of("key", "sourceData", "label", "Source Data", "type", "text", "default", "json"),
                                        Map.of("key", "dataPropertyName", "label", "Property Name", "type", "text", "default", "data"),
                                        Map.of("key", "extractionValues", "label", "Extraction Values", "type", "json"),
                                        Map.of("key", "options", "label", "Options", "type", "json")
                                )
                        ),
                        Map.of(
                                "actionKey", "html:convertToHtmlTable",
                                "name", "Convert to HTML Table",
                                "description", "Convert to HTML Table",
                                "configSchema", List.of(
                                        Map.of("key", "options", "label", "Options", "type", "json")
                                )
                        )
                )
        ).credentialSchema(List.of()).category("data-transformation");
    }
}
