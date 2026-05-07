package com.crescendo.apps.jokeapi;

import com.crescendo.app.App;
import com.crescendo.apps.AppDefinition;
import com.crescendo.enums.AuthType;
import org.springframework.stereotype.Component;

import java.util.List;
import java.util.Map;

@Component
public class JokeApiApp implements AppDefinition {

    @Override
    public App toApp() {
        return new App("joke-api", "Joke API", "Get random jokes from JokeAPI",
                "/icons/jokeapi.svg", AuthType.NONE,
                List.of(),
                List.of(Map.of(
                    "actionKey", "get-joke",
                    "name", "Get Random Joke",
                    "description", "Retrieve a random joke",
                    "configSchema", List.of(
                        Map.of("key", "category", "label", "Category",
                               "type", "select", "required", false,
                               "options", List.of(
                                   Map.of("value", "", "label", "Any"),
                                   Map.of("value", "Programming", "label", "Programming"),
                                   Map.of("value", "Misc", "label", "Miscellaneous"),
                                   Map.of("value", "Pun", "label", "Pun"),
                                   Map.of("value", "Spooky", "label", "Spooky"),
                                   Map.of("value", "Christmas", "label", "Christmas")
                               ),
                               "helpText", "Filter jokes by category")
                    )
                ))
        )
        .credentialSchema(List.of())
        .category("fun")
        .helpUrl("https://v2.jokeapi.dev/");
    }
}
