package com.crescendo.apps.markdown;

import com.crescendo.app.App;
import com.crescendo.apps.AppDefinition;
import com.crescendo.enums.AuthType;
import org.springframework.stereotype.Component;

import java.util.List;
import java.util.Map;

/**
 * AppDefinition for Markdown.
 */
@Component
public class MarkdownApp implements AppDefinition {

    @Override
    public App toApp() {
        return new App(
                "markdown",
                "Markdown",
                """
                Convert data between Markdown and HTML.
                
                This integration provides operations for:
                - **HTML to Markdown**: Convert HTML to Markdown format
                - **Markdown to HTML**: Convert Markdown to HTML format
                """,
                "/icons/markdown.svg", // Generic icon
                AuthType.NONE,
                List.of(),
                List.of(
                        Map.of(
                                "actionKey", "markdown:convert",
                                "name", "Convert HTML/Markdown",
                                "description", "Convert data between Markdown and HTML",
                                "configSchema", List.of(
                                        Map.of("key", "mode", "label", "Mode", "type", "text", "default", "htmlToMarkdown"),
                                        Map.of("key", "html", "label", "HTML", "type", "text"),
                                        Map.of("key", "markdown", "label", "Markdown", "type", "text"),
                                        Map.of("key", "destinationKey", "label", "Destination Key", "type", "text", "default", "data"),
                                        Map.of("key", "options", "label", "Options", "type", "json")
                                )
                        )
                )
        ).credentialSchema(List.of()).category("data-transformation");
    }
}
