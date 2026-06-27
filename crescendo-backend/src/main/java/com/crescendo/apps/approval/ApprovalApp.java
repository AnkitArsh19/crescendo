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
        return new App("approval", "Approval", """
                The Crescendo Approval app is a built-in utility that allows you to pause a workflow mid-execution until a human provides explicit approval.

                **What you can do with Approval in Crescendo:**
                - Pause a CI/CD deployment pipeline until an engineering manager clicks "Approve"
                - Send a generated contract draft to a legal team via email, waiting for their sign-off before sending to the client
                - Request expense report approval in Slack, resuming the workflow only if accepted
                - Implement manual quality-assurance steps in AI-generated content generation

                **Actions available:**
                - Request Approval — pauses the current workflow and generates a unique, secure approval URL. The workflow will remain suspended until a user visits the URL and selects either "Approve" or "Reject".

                **Who should use this:** Operations managers, HR teams, and developers building secure, human-in-the-loop automation processes.

                **Authentication:** None required.
                """,
                "/icons/approval.png", AuthType.NONE,
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
