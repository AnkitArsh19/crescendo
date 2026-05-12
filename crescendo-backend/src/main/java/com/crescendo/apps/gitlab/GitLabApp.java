package com.crescendo.apps.gitlab;

import com.crescendo.app.App;
import com.crescendo.apps.AppDefinition;
import com.crescendo.enums.AuthType;
import org.springframework.stereotype.Component;
import java.util.List;
import java.util.Map;

@Component
public class GitLabApp implements AppDefinition {
    @Override
    public App toApp() {
        var projField = Map.of("key", "projectId", "label", "Project",
                "type", "dynamic_dropdown", "resourceType", "projects",
                "required", true, "helpText", "Select the GitLab project");

        return new App("gitlab", "GitLab", "Manage issues, merge requests, and pipelines in GitLab",
                "/icons/gitlab.svg", AuthType.OAUTH2,
                List.of(
                    Map.of("triggerKey", "new-issue", "name", "New Issue",
                        "description", "Triggers when a new issue is created",
                        "configSchema", List.of(projField)),
                    Map.of("triggerKey", "new-mr", "name", "New Merge Request",
                        "description", "Triggers when a new MR is opened",
                        "configSchema", List.of(projField)),
                    Map.of("triggerKey", "push", "name", "New Push",
                        "description", "Triggers on code push",
                        "configSchema", List.of(projField,
                            Map.of("key", "branch", "label", "Branch", "type", "text", "required", false,
                                   "helpText", "Specific branch or leave empty for all"))),
                    Map.of("triggerKey", "new-comment", "name", "New Comment",
                        "description", "Triggers on a new comment",
                        "configSchema", List.of(projField)),
                    Map.of("triggerKey", "pipeline-status", "name", "Pipeline Status Changed",
                        "description", "Triggers when a CI/CD pipeline status changes",
                        "configSchema", List.of(projField))
                ),
                List.of(
                    Map.of("actionKey", "create-issue", "name", "Create Issue",
                        "description", "Open a new GitLab issue",
                        "configSchema", List.of(projField,
                            Map.of("key", "title", "label", "Title", "type", "text", "required", true,
                                   "placeholder", "Fix login bug", "helpText", "Issue title"),
                            Map.of("key", "description", "label", "Description", "type", "textarea",
                                   "required", false, "helpText", "Description (Markdown)"),
                            Map.of("key", "labels", "label", "Labels", "type", "text", "required", false,
                                   "placeholder", "bug, frontend", "helpText", "Comma-separated labels"))),
                    Map.of("actionKey", "create-mr", "name", "Create Merge Request",
                        "description", "Open a new merge request",
                        "configSchema", List.of(projField,
                            Map.of("key", "title", "label", "Title", "type", "text", "required", true, "helpText", "MR title"),
                            Map.of("key", "sourceBranch", "label", "Source Branch", "type", "text", "required", true, "helpText", "Branch with changes"),
                            Map.of("key", "targetBranch", "label", "Target Branch", "type", "text", "required", true,
                                   "placeholder", "main", "helpText", "Branch to merge into"),
                            Map.of("key", "description", "label", "Description", "type", "textarea", "required", false, "helpText", "MR description"))),
                    Map.of("actionKey", "add-comment", "name", "Add Comment",
                        "description", "Comment on an issue or MR",
                        "configSchema", List.of(projField,
                            Map.of("key", "issueIid", "label", "Issue/MR ID", "type", "text", "required", true, "helpText", "Issue or MR internal ID"),
                            Map.of("key", "body", "label", "Comment", "type", "textarea", "required", true, "helpText", "Comment body"))),
                    Map.of("actionKey", "close-issue", "name", "Close Issue",
                        "description", "Close an existing issue",
                        "configSchema", List.of(projField,
                            Map.of("key", "issueIid", "label", "Issue ID", "type", "text", "required", true, "helpText", "Internal issue ID")))
                )
        ).credentialSchema(List.of()).category("developer").helpUrl("https://gitlab.com/-/user_settings/applications");
    }
}
