package com.crescendo.apps.json;

import com.crescendo.app.App;
import com.crescendo.apps.AppDefinition;
import com.crescendo.enums.AuthType;
import org.springframework.stereotype.Component;

import java.util.List;
import java.util.Map;

@Component
public class JsonApp implements AppDefinition {
    @Override
    public App toApp() {
        return new App("json", "JSON", """
                The JSON app is a native object utility that allows you to parse strings into JSON and stringify JSON objects back into strings.
                
                **What you can do with JSON in Crescendo:**
                - Extract JSON strings hidden inside HTTP webhooks
                - Stringify a complex array to pass it as a single string variable to another tool
                - Keep data clean by assigning parsed outputs directly to target properties

                **Actions available:**
                - Parse — convert a JSON string into a native object
                - Stringify — convert a native object into a JSON string

                **Authentication:** None required.
                """,
                "/icons/json.png", AuthType.NONE,
                List.of(),
                List.of(
                        Map.of("actionKey", "parse", "name", "Parse JSON",
                                "description", "Parse a string into a JSON object",
                                "configSchema", List.of(
                                        Map.of("key", "propertyName", "label", "Property Name", "type", "text", "required", true,
                                                "default", "data", "helpText", "The property containing the string to parse"),
                                        Map.of("key", "destinationProperty", "label", "Destination Property", "type", "text", "required", false,
                                                "helpText", "Where to save the parsed JSON. Defaults to the same property if blank.")
                                )),
                        Map.of("actionKey", "stringify", "name", "Stringify JSON",
                                "description", "Stringify a JSON object into a string",
                                "configSchema", List.of(
                                        Map.of("key", "propertyName", "label", "Property Name", "type", "text", "required", true,
                                                "default", "data", "helpText", "The property containing the JSON object to stringify"),
                                        Map.of("key", "destinationProperty", "label", "Destination Property", "type", "text", "required", false,
                                                "helpText", "Where to save the string. Defaults to the same property if blank.")
                                ))
                )
        ).credentialSchema(List.of()).category("core").helpUrl("");
    }
}
