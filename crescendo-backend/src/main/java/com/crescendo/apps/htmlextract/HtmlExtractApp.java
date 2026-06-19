package com.crescendo.apps.htmlextract;

import com.crescendo.app.App;
import com.crescendo.apps.AppDefinition;
import com.crescendo.enums.AuthType;
import org.springframework.stereotype.Component;

import java.util.List;
import java.util.Map;

@Component
public class HtmlExtractApp implements AppDefinition {
    @Override
    public App toApp() {
        return new App("html-extract", "HTML Extract", "Extract text or attributes from HTML with simple selectors",
                "/icons/html.svg", AuthType.NONE, List.of(), List.of(
                Map.of("actionKey", "extract", "name", "Extract",
                        "description", "Extract matching text from HTML using tag, #id, or .class selectors",
                        "configSchema", List.of(
                                Map.of("key", "html", "label", "HTML", "type", "textarea", "required", true),
                                Map.of("key", "selector", "label", "Selector", "type", "text", "required", true,
                                        "placeholder", "h1, #title, .price"),
                                Map.of("key", "attribute", "label", "Attribute", "type", "text", "required", false,
                                        "placeholder", "href"))))
        ).credentialSchema(List.of()).category("core").helpUrl("");
    }
}
