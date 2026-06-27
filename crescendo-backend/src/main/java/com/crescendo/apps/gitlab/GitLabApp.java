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

        return new App("gitlab", "GitLab", """
                GitLab is a web-based Git repository that provides open-source DevOps and issue tracking. The Crescendo GitLab app allows you to manage issues, merge requests, and pipelines automatically.

                **What you can do with GitLab in Crescendo:**
                - Automatically close GitLab issues when a Jira ticket is resolved
                - Send a Discord notification when a new Merge Request is opened
                - Create new issues directly from Slack threads
                - Add notes to MRs automatically based on code analysis results

                **Actions available:**
                - Create Issue — open a new ticket
                - Close Issue — mark an issue as resolved
                - Create Merge Request — propose code changes
                - Create Note — add a comment to an issue or MR

                **Who should use this:** Engineering teams, QA testers, and DevOps engineers managing codebases and deployments.

                **Authentication:** Personal Access Token or OAuth 2.0.
                """,
                "https://www.google.com/s2/favicons?domain=gitlab.com&sz=128", AuthType.OAUTH2,
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
                    Map.of("actionKey", "gitlab:issue:create", "name", "Create Issue",
                        "description", "Open a new GitLab issue",
                        "configSchema", List.of(projField,
                            Map.of("key", "title", "label", "Title", "type", "text", "required", true,
                                   "placeholder", "Fix login bug", "helpText", "Issue title"),
                            Map.of("key", "description", "label", "Description", "type", "textarea",
                                   "required", false, "helpText", "Description (Markdown)"),
                            Map.of("key", "labels", "label", "Labels", "type", "text", "required", false,
                                   "placeholder", "bug, frontend", "helpText", "Comma-separated labels"))),
                    Map.of("actionKey", "gitlab:mergeRequest:create", "name", "Create Merge Request",
                        "description", "Open a new merge request",
                        "configSchema", List.of(projField,
                            Map.of("key", "title", "label", "Title", "type", "text", "required", true, "helpText", "MR title"),
                            Map.of("key", "sourceBranch", "label", "Source Branch", "type", "text", "required", true, "helpText", "Branch with changes"),
                            Map.of("key", "targetBranch", "label", "Target Branch", "type", "text", "required", true,
                                   "placeholder", "main", "helpText", "Branch to merge into"),
                            Map.of("key", "description", "label", "Description", "type", "textarea", "required", false, "helpText", "MR description"))),
                    Map.of("actionKey", "gitlab:issue:update", "name", "Add Comment",
                        "description", "Comment on an issue or MR",
                        "configSchema", List.of(projField,
                            Map.of("key", "issueIid", "label", "Issue/MR ID", "type", "text", "required", true, "helpText", "Issue or MR internal ID"),
                            Map.of("key", "body", "label", "Comment", "type", "textarea", "required", true, "helpText", "Comment body"))),
                    Map.of("actionKey", "gitlab:issue:update", "name", "Close Issue",
                        "description", "Close an existing issue",
                        "configSchema", List.of(projField,
                            Map.of("key", "issueIid", "label", "Issue ID", "type", "text", "required", true, "helpText", "Internal issue ID")))
                )
        ).credentialSchema(List.of()).altAuthType(AuthType.APIKEY).category("developer").helpUrl("https://gitlab.com/-/user_settings/applications");
    }
}
