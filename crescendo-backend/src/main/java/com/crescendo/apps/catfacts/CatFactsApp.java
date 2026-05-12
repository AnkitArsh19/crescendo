package com.crescendo.apps.catfacts;

import com.crescendo.app.App;
import com.crescendo.apps.AppDefinition;
import com.crescendo.enums.AuthType;
import org.springframework.stereotype.Component;
import java.util.List;
import java.util.Map;

@Component
public class CatFactsApp implements AppDefinition {
    @Override
    public App toApp() {
        return new App("cat-facts", "Cat Facts", "Get random cat facts",
                "/icons/catfacts.svg", AuthType.NONE,
                List.of(),
                List.of(
                    Map.of("actionKey", "get-fact", "name", "Get Random Cat Fact",
                        "description", "Retrieve a single random cat fact",
                        "configSchema", List.of()),
                    Map.of("actionKey", "get-multiple-facts", "name", "Get Multiple Facts",
                        "description", "Retrieve multiple cat facts at once",
                        "configSchema", List.of(
                            Map.of("key", "limit", "label", "Number of Facts", "type", "text", "required", false,
                                   "placeholder", "5", "helpText", "How many facts (default: 5)")))
                )
        ).credentialSchema(List.of()).category("fun").helpUrl("https://catfact.ninja/");
    }
}
