package com.crescendo.apps.markdown;

import com.crescendo.app.App;
import com.crescendo.apps.AppDefinition;
import com.crescendo.enums.AuthType;
import org.springframework.stereotype.Component;

import java.util.List;
import java.util.Map;

@Component
public class MarkdownApp implements AppDefinition {
    @Override
    public App toApp() {
        return new App("markdown", "Markdown", "Convert Markdown to HTML",
                "/icons/markdown.svg", AuthType.NONE,
                List.of(),
                List.of(
                    Map.of("actionKey", "to-html", "name", "Markdown to HTML",
                        "description", "Converts a Markdown string to HTML",
                        "configSchema", List.of(
                            Map.of("key", "text", "label", "Markdown Text", "type", "text", "required", true,
                                   "placeholder", "# Hello World", "helpText", "The markdown text to convert")
                        ))
                )
        ).credentialSchema(List.of()).category("core").helpUrl("");
    }
}
