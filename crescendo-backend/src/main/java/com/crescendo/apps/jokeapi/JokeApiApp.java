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
                List.of(
                    Map.of("actionKey", "get-joke", "name", "Get Random Joke",
                        "description", "Retrieve a random joke",
                        "configSchema", List.of(
                            Map.of("key", "category", "label", "Category", "type", "select", "required", false,
                                   "options", List.of(
                                       Map.of("value", "Any", "label", "Any"),
                                       Map.of("value", "Programming", "label", "Programming"),
                                       Map.of("value", "Misc", "label", "Miscellaneous"),
                                       Map.of("value", "Dark", "label", "Dark"),
                                       Map.of("value", "Pun", "label", "Pun"),
                                       Map.of("value", "Spooky", "label", "Spooky"),
                                       Map.of("value", "Christmas", "label", "Christmas")
                                   ), "helpText", "Joke category"),
                            Map.of("key", "type", "label", "Type", "type", "select", "required", false,
                                   "options", List.of(
                                       Map.of("value", "", "label", "Any"),
                                       Map.of("value", "single", "label", "Single"),
                                       Map.of("value", "twopart", "label", "Two-Part (setup + delivery)")
                                   ), "helpText", "Joke format"),
                            Map.of("key", "language", "label", "Language", "type", "select", "required", false,
                                   "options", List.of(
                                       Map.of("value", "en", "label", "English"),
                                       Map.of("value", "de", "label", "German"),
                                       Map.of("value", "es", "label", "Spanish"),
                                       Map.of("value", "fr", "label", "French"),
                                       Map.of("value", "pt", "label", "Portuguese")
                                   ), "helpText", "Joke language")))
                )
        ).credentialSchema(List.of()).category("fun").helpUrl("https://v2.jokeapi.dev/");
    }
}
