package com.crescendo.apps.typeform;

import com.crescendo.app.App;
import com.crescendo.apps.AppDefinition;
import com.crescendo.enums.AuthType;
import org.springframework.stereotype.Component;
import java.util.*;

@Component
public class TypeformApp implements AppDefinition {
    public App toApp() {
        return new App(
                "typeform",
                "Typeform",
                "Read Typeform forms and responses",
                "https://www.google.com/s2/favicons?domain=typeform.com&sz=128",
                AuthType.OAUTH2,
                List.of(
                        Map.of(
                                "triggerKey", "form-submit",
                                "name", "Form Submitted",
                                "description", "Triggers from a Typeform webhook",
                                "configSchema", List.of(
                                        Map.of("key", "formId", "label", "Form ID", "type", "text", "required", true)
                                )
                        )
                ),
                List.of(
                        Map.of(
                                "actionKey", "list-forms",
                                "name", "List Forms",
                                "description", "List forms",
                                "configSchema", List.of()
                        ),
                        Map.of(
                                "actionKey", "list-responses",
                                "name", "List Responses",
                                "description", "List responses for a form",
                                "configSchema", List.of(
                                        Map.of("key", "formId", "label", "Form ID", "type", "text", "required", true),
                                        Map.of("key", "pageSize", "label", "Page Size", "type", "text", "required", false, "placeholder", "25")
                                )
                        )
                )
        ).credentialSchema(List.of()).altAuthType(AuthType.OAUTH2).category("forms").helpUrl("https://www.typeform.com/developers/");
    }
}
