package com.crescendo.apps.renamekeys;

import com.crescendo.app.App;
import com.crescendo.apps.AppDefinition;
import com.crescendo.enums.AuthType;
import org.springframework.stereotype.Component;

import java.util.List;
import java.util.Map;

/**
 * AppDefinition for Rename Keys.
 */
@Component
public class RenameKeysApp implements AppDefinition {

    @Override
    public App toApp() {
        return new App(
                "renameKeys",
                "Rename Keys",
                """
                Rename Keys is a data-transformation utility that helps you rename fields within a JSON object dynamically.
                
                **What you can do with Rename Keys in Crescendo:**
                - Reformat payload keys before sending them to a strict API endpoint
                - Normalize incoming webhook data to match internal schemas
                - Transform complex JSON structures easily
                
                **Actions available:**
                - Rename — supply a mapping of old keys to new keys and apply it to a JSON object
                
                **Who should use this:** Integration engineers and developers dealing with disparate API schemas.
                
                **Authentication:** None required.
                """,
                "/icons/rename.svg", // Generic icon
                AuthType.NONE,
                List.of(),
                List.of(
                        Map.of(
                                "actionKey", "renameKeys:rename",
                                "name", "Rename",
                                "description", "Update item field names",
                                "configSchema", List.of(
                                        Map.of("key", "keys", "label", "Keys", "type", "json"),
                                        Map.of("key", "additionalOptions", "label", "Additional Options", "type", "json")
                                )
                        )
                )
        ).credentialSchema(List.of()).category("data-transformation");
    }
}
