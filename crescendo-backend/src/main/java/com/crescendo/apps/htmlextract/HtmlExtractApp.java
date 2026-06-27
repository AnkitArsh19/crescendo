package com.crescendo.apps.htmlextract;

import com.crescendo.app.App;
import com.crescendo.apps.AppDefinition;
import com.crescendo.enums.AuthType;
import org.springframework.stereotype.Component;

import java.util.List;
import java.util.Map;

/**
 * AppDefinition for HTML Extract.
 */
@Component
public class HtmlExtractApp implements AppDefinition {

    @Override
    public App toApp() {
        return new App(
                "htmlExtract",
                "HTML Extract",
                """
                HTML Extract allows you to easily parse and extract specific data from raw HTML strings or web pages using CSS selectors.
                
                **What you can do with HTML Extract in Crescendo:**
                - Scrape prices or product details from an e-commerce page
                - Extract article titles and metadata from news websites
                - Pull data from internal dashboards that lack a formal API
                
                **Actions available:**
                - Extract — extract data from HTML by specifying the target properties and CSS selectors
                
                **Who should use this:** Data analysts, QA engineers, and developers dealing with web scraping.
                
                **Authentication:** None required.
                """,
                "https://www.google.com/s2/favicons?domain=developer.mozilla.org&sz=128", // Generic icon
                AuthType.NONE,
                List.of(),
                List.of(
                        Map.of(
                                "actionKey", "htmlExtract:extract",
                                "name", "Extract",
                                "description", "Extract data from HTML",
                                "configSchema", List.of(
                                        Map.of("key", "sourceData", "label", "Source Data", "type", "text", "default", "json"),
                                        Map.of("key", "dataPropertyName", "label", "Property Name", "type", "text", "default", "data"),
                                        Map.of("key", "extractionValues", "label", "Extraction Values", "type", "json"),
                                        Map.of("key", "options", "label", "Options", "type", "json")
                                )
                        )
                )
        ).credentialSchema(List.of()).category("data-transformation").internal(true);
    }
}
