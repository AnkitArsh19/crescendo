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
        return new App("cat-facts", "Cat Facts", "Get random cat facts from the Cat Facts API",
                "/icons/catfacts.svg", AuthType.NONE,
                List.of(),
                List.of(Map.of(
                    "actionKey", "get-fact",
                    "name", "Get Random Cat Fact",
                    "description", "Retrieve a random cat fact"
                ))
        )
        .credentialSchema(List.of())
        .category("fun")
        .helpUrl("https://catfact.ninja/");
    }
}
