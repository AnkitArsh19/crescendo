package com.crescendo.apps.renamekeys;

import com.crescendo.app.App;
import com.crescendo.apps.AppDefinition;
import com.crescendo.enums.AuthType;
import org.springframework.stereotype.Component;

import java.util.List;
import java.util.Map;

@Component
public class RenameKeysApp implements AppDefinition {
    @Override
    public App toApp() {
        return new App("rename-keys", "Rename Keys", "Rename keys in the incoming JSON data",
                "/icons/rename-keys.svg", AuthType.NONE,
                List.of(),
                List.of(
                    Map.of("actionKey", "rename", "name", "Rename Keys",
                        "description", "Map old keys to new keys",
                        "configSchema", List.of(
                            Map.of("key", "keyMap", "label", "Key Map (JSON)", "type", "json", "required", true,
                                   "placeholder", "{\"old_key_name\": \"newKeyName\"}", 
                                   "helpText", "Provide a JSON object where the key is the old name and the value is the new name.")
                        ))
                )
        ).credentialSchema(List.of()).category("core").helpUrl("");
    }
}
