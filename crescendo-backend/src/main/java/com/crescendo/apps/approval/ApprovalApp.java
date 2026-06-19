package com.crescendo.apps.approval;

import com.crescendo.app.App;
import com.crescendo.apps.AppDefinition;
import com.crescendo.enums.AuthType;
import org.springframework.stereotype.Component;

import java.util.List;
import java.util.Map;

@Component
public class ApprovalApp implements AppDefinition {

    @Override
    public App toApp() {
        return new App("approval", "Approval", "Pause a workflow until a public approval response is submitted",
                "/icons/approval.svg", AuthType.NONE,
                List.of(),
                List.of(
                        Map.of(
                                "actionKey", "request-approval",
                                "name", "Request Approval",
                                "description", "Create a public approval response link and pause until it is submitted",
                                "configSchema", List.of(
                                        Map.of("key", "title", "label", "Title", "type", "text", "required", true,
                                                "placeholder", "Approve this request"),
                                        Map.of("key", "message", "label", "Message", "type", "textarea", "required", false,
                                                "placeholder", "Review the workflow data before continuing"),
                                        Map.of("key", "fields", "label", "Response Fields (JSON)", "type", "json", "required", false,
                                                "placeholder", "[{\"key\":\"comment\",\"label\":\"Comment\",\"type\":\"textarea\",\"required\":false}]"),
                                        Map.of("key", "successMessage", "label", "Success Message", "type", "text", "required", false,
                                                "placeholder", "Thanks, your response was recorded")
                                )
                        )
                )
        ).credentialSchema(List.of()).category("core").helpUrl("");
    }
}
