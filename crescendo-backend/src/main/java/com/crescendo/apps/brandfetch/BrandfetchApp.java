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
                "https://www.google.com/s2/favicons?domain=brandfetch.com&sz=128",
                AuthType.NONE,
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
        ).credentialSchema(List.of()).category("data").helpUrl("https://docs.brandfetch.com/");
    }
}
