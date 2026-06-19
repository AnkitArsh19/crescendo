package com.crescendo.apps.html;

import com.crescendo.app.App;
import com.crescendo.apps.AppDefinition;
import com.crescendo.enums.AuthType;
import org.springframework.stereotype.Component;

import java.util.List;
import java.util.Map;

@Component
public class HtmlGenerateApp implements AppDefinition {
    @Override
    public App toApp() {
        return new App("html-generate", "HTML", "Generate HTML content",
                "/icons/html.svg", AuthType.NONE,
                List.of(),
                List.of(
                    Map.of("actionKey", "generate", "name", "Generate HTML",
                        "description", "Create an HTML string using templates",
                        "configSchema", List.of(
                            Map.of("key", "html", "label", "HTML Content", "type", "text", "required", true,
                                   "placeholder", "<h1>Hello {{steps.1.name}}</h1>", "helpText", "The HTML structure")
                        ))
                )
        ).credentialSchema(List.of()).category("core").helpUrl("");
    }
}
