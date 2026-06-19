package com.crescendo.apps.googleforms;

import com.crescendo.app.App;
import com.crescendo.apps.AppDefinition;
import com.crescendo.enums.AuthType;
import org.springframework.stereotype.Component;

import java.util.List;
import java.util.Map;

@Component
public class GoogleFormsApp implements AppDefinition {

    @Override
    public App toApp() {
        return new App("google-forms", "Google Forms", "Create forms and collect responses",
                "https://ssl.gstatic.com/images/branding/product/2x/forms_2020q4_48dp.png", AuthType.OAUTH2,
                List.of(Map.of(
                    "triggerKey", "new-response",
                    "name", "New Response",
                    "description", "Triggers when a new form response is submitted",
                    "configSchema", List.of(
                        Map.of("key", "formId", "label", "Form",
                               "type", "dynamic_dropdown", "resourceType", "forms",
                               "required", true,
                               "helpText", "Select the form to watch for responses")
                    )
                )),
                List.of(
                    Map.of(
                        "actionKey", "create-form",
                        "name", "Create Form",
                        "description", "Create a new Google Form",
                        "configSchema", List.of(
                            Map.of("key", "title", "label", "Form Title",
                                   "type", "text", "required", true,
                                   "placeholder", "Customer Feedback",
                                   "helpText", "Title of the new form"),
                            Map.of("key", "documentTitle", "label", "Drive File Name",
                                   "type", "text", "required", false,
                                   "placeholder", "feedback-form",
                                   "helpText", "Name in Google Drive (defaults to form title)")
                        )
                    ),
                    Map.of(
                        "actionKey", "list-responses",
                        "name", "List Responses",
                        "description", "Retrieve responses from a form",
                        "configSchema", List.of(
                            Map.of("key", "formId", "label", "Form",
                                   "type", "dynamic_dropdown", "resourceType", "forms",
                                   "required", true,
                                   "helpText", "Select the form to get responses from"),
                            Map.of("key", "pageSize", "label", "Max Results",
                                   "type", "text", "required", false,
                                   "placeholder", "20",
                                   "helpText", "Maximum responses to return")
                        )
                    ),
                    Map.of(
                        "actionKey", "get-form",
                        "name", "Get Form",
                        "description", "Get form metadata and questions",
                        "configSchema", List.of(
                            Map.of("key", "formId", "label", "Form",
                                   "type", "dynamic_dropdown", "resourceType", "forms",
                                   "required", true,
                                   "helpText", "Select the form to retrieve"))
                    )
                )
        )
        .credentialSchema(List.of())
        .category("productivity")
        .helpUrl("https://console.cloud.google.com/");
    }
}