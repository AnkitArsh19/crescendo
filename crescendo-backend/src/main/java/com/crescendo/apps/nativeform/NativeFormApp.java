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
        return new App("native-form", "Form Trigger", """
                Native Form is Crescendo's built-in, no-code form builder. The Crescendo Native Form app lets you generate public-facing forms instantly and trigger workflows the moment they are submitted.

                **What you can do with Native Form in Crescendo:**
                - Create a quick "Contact Us" form and route submissions to Freshdesk
                - Collect survey answers and append them directly to Microsoft Excel
                - Build a simple registration page that adds users to a Mailchimp audience
                - Gather feature requests and automatically open GitHub issues

                **Triggers available:**
                - Form Submitted — start a workflow whenever a user hits submit on your public form URL

                **Who should use this:** Anyone who needs to quickly collect structured data without paying for external form providers or writing HTML.

                **Authentication:** None required. Forms are public by default.
                """,
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
