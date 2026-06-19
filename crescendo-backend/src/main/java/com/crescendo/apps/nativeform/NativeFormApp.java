package com.crescendo.apps.nativeform;

import com.crescendo.app.App;
import com.crescendo.apps.AppDefinition;
import com.crescendo.enums.AuthType;
import org.springframework.stereotype.Component;

import java.util.List;
import java.util.Map;

@Component
public class NativeFormApp implements AppDefinition {

    @Override
    public App toApp() {
        return new App("native-form", "Form Trigger", "Start a workflow from a public no-code form",
                "/icons/form.svg", AuthType.NONE,
                List.of(
                        Map.of(
                                "triggerKey", "form-submit",
                                "name", "Form Submitted",
                                "description", "Triggers when a public Crescendo form receives a submission",
                                "configSchema", List.of(
                                        Map.of("key", "formKey", "label", "Form Key", "type", "text", "required", true,
                                                "placeholder", "lead-intake"),
                                        Map.of("key", "title", "label", "Title", "type", "text", "required", false,
                                                "placeholder", "Lead intake"),
                                        Map.of("key", "fields", "label", "Fields (JSON)", "type", "json", "required", true,
                                                "placeholder", "[{\"key\":\"email\",\"label\":\"Email\",\"type\":\"email\",\"required\":true}]"),
                                        Map.of("key", "successMessage", "label", "Success Message", "type", "text", "required", false,
                                                "placeholder", "Thanks, your form was submitted")
                                )
                        )
                ),
                List.of()
        ).credentialSchema(List.of()).category("core").helpUrl("");
    }
}
