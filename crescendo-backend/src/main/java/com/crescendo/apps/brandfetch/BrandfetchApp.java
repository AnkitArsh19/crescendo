package com.crescendo.apps.brandfetch;

import com.crescendo.app.App;
import com.crescendo.apps.AppDefinition;
import com.crescendo.enums.AuthType;
import org.springframework.stereotype.Component;
import java.util.*;

@Component
public class BrandfetchApp implements AppDefinition {
    public App toApp() {
        return new App(
                "brandfetch",
                "Brandfetch",
                "Fetch brand logos, colors, and fonts by domain",
                "/icons/brandfetch.svg",
                AuthType.APIKEY,
                List.of(),
                List.of(
                        Map.of(
                                "actionKey", "get-brand",
                                "name", "Get Brand",
                                "description", "Fetch brand profile by domain",
                                "configSchema", List.of(
                                        Map.of("key", "domain", "label", "Domain", "type", "text", "required", true, "placeholder", "openai.com")
                                )
                        )
                )
        ).credentialSchema(List.of(
                Map.of("key", "apiKey", "label", "API Key", "type", "password", "required", true)
        )).category("data").helpUrl("https://docs.brandfetch.com/");
    }
}
