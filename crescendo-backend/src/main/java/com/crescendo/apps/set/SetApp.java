package com.crescendo.apps.set;

import com.crescendo.app.App;
import com.crescendo.apps.AppDefinition;
import com.crescendo.enums.AuthType;
import org.springframework.stereotype.Component;

import java.util.List;
import java.util.Map;

@Component
public class SetApp implements AppDefinition {
    @Override
    public App toApp() {
        return new App("set", "Set", "Set, rename, or remove fields in the workflow data",
                "/icons/set.svg", AuthType.NONE,
                List.of(),
                List.of(
                    Map.of("actionKey", "edit-fields", "name", "Edit Fields",
                        "description", "Set custom fields for the next step",
                        "configSchema", List.of(
                            Map.of("key", "keepOnlySet", "label", "Keep Only Set", "type", "boolean", "required", false,
                                   "helpText", "If true, discards all other input data and keeps only the fields defined here. Defaults to false (merges fields)."),
                            Map.of("key", "fields", "label", "Fields (JSON)", "type", "json", "required", true,
                                   "placeholder", "{\"myKey\": \"myValue\"}", "helpText", "Provide a JSON object containing the fields to set")
                        ))
                )
        ).credentialSchema(List.of()).category("core").helpUrl("");
    }
}
