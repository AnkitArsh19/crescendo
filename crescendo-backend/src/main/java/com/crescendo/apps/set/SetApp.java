package com.crescendo.apps.set;

import com.crescendo.app.App;
import com.crescendo.apps.AppDefinition;
import com.crescendo.enums.AuthType;
import org.springframework.stereotype.Component;

import java.util.List;
import java.util.Map;

/**
 * AppDefinition for Set.
 */
@Component
public class SetApp implements AppDefinition {

    @Override
    public App toApp() {
        return new App(
                "set",
                "Set",
                """
                Modify, add, or remove item fields.
                
                This integration provides operations for:
                - **Set**: Add or edit fields on an input item and optionally remove other fields
                """,
                "/icons/set.svg", // Generic icon
                AuthType.NONE,
                List.of(),
                List.of(
                        Map.of(
                                "actionKey", "set:set",
                                "name", "Set",
                                "description", "Modify, add, or remove item fields",
                                "configSchema", List.of(
                                        Map.of("key", "mode", "label", "Mode", "type", "text", "default", "manual"),
                                        Map.of("key", "jsonOutput", "label", "JSON Output", "type", "text"),
                                        Map.of("key", "fields", "label", "Fields to Set", "type", "json"),
                                        Map.of("key", "options", "label", "Options", "type", "json")
                                )
                        )
                )
        ).credentialSchema(List.of()).category("data-transformation");
    }
}
