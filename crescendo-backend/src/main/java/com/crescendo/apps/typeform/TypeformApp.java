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
                "Typeform", """
                Typeform is a web-based platform for creating forms and surveys that are interactive and engaging.
                
                This integration supports:
                - **Get Forms**: Retrieve a list of forms from your account
                - **Get Responses**: Retrieve responses for a specific form
                - **Trigger on Submission**: Get notified instantly when a form is submitted (Webhook)
                
                Authenticate using a Personal Access Token or OAuth2.
                """,
                "https://www.google.com/s2/favicons?domain=typeform.com&sz=128",
                AuthType.OAUTH2,
                List.of(
                        Map.of(
                                "triggerKey", "typeform:webhook:subscribe",
                                "name", "Form Submitted",
                                "description", "Triggers when a specific form is submitted via Webhook",
                                "configSchema", List.of(
                                        Map.of("key", "formId", "label", "Form ID", "type", "text", "required", true)
                                )
                        )
                ),
                List.of(
                        Map.of(
                                "actionKey", "typeform:form:getAll",
                                "name", "Get Forms",
                                "description", "Retrieve a list of forms",
                                "configSchema", List.of()
                        ),
                        Map.of(
                                "actionKey", "typeform:response:getAll",
                                "name", "Get Responses",
                                "description", "Retrieve responses for a specific form",
                                "configSchema", List.of(
                                        Map.of("key", "formId", "label", "Form ID", "type", "text", "required", true)
                                )
                        )
                )
        ).credentialSchema(List.of(
                Map.of("key", "accessToken", "label", "Personal Access Token", "type", "password", "required", true)
        )).altAuthType(AuthType.OAUTH2).category("marketing");
    }
}
