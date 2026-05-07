package com.crescendo.apps.github;

import com.crescendo.app.App;
import com.crescendo.apps.AppDefinition;
import com.crescendo.enums.AuthType;
import org.springframework.stereotype.Component;

import java.util.List;
import java.util.Map;

@Component
public class GitHubApp implements AppDefinition {

    @Override
    public App toApp() {
        var repoField = Map.of("key", "repo", "label", "Repository",
                "type", "dynamic_dropdown", "resourceType", "repos",
                "required", true,
                "helpText", "Select the repository");

        var branchField = Map.<String, Object>of("key", "branch", "label", "Branch",
                "type", "dynamic_dropdown", "resourceType", "branches",
                "dependsOn", List.of("repo"),
                "required", false,
                "helpText", "Optionally filter to a specific branch");

        return new App("github", "GitHub", "Create issues, PRs, and manage repositories",
                "/icons/github.svg", AuthType.OAUTH2,

                // ═══ TRIGGERS ═══
                List.of(
                    Map.of(
                        "triggerKey", "push",
                        "name", "Push Event",
                        "description", "Triggers on a push to a repository",
                        "configSchema", List.of(repoField, branchField)
                    ),
                    Map.of(
                        "triggerKey", "new-issue",
                        "name", "New Issue",
                        "description", "Triggers when a new issue is created in a repository",
                        "configSchema", List.of(repoField)
                    ),
                    Map.of(
                        "triggerKey", "new-pr",
                        "name", "New Pull Request",
                        "description", "Triggers when a new pull request is opened",
                        "configSchema", List.of(repoField)
                    ),
                    Map.of(
                        "triggerKey", "new-star",
                        "name", "New Star",
                        "description", "Triggers when someone stars the repository",
                        "configSchema", List.of(repoField)
                    )
                ),

                // ═══ ACTIONS ═══
                List.of(
                    Map.of(
                        "actionKey", "create-issue",
                        "name", "Create Issue",
                        "description", "Create a new issue in a repository",
                        "configSchema", List.of(
                            repoField,
                            Map.of("key", "title", "label", "Issue Title",
                                   "type", "text", "required", true,
                                   "placeholder", "Bug: ...",
                                   "helpText", "The title for the new issue"),
                            Map.of("key", "body", "label", "Issue Body",
                                   "type", "textarea", "required", false,
                                   "placeholder", "Describe the issue...",
                                   "helpText", "Issue body in Markdown"),
                            Map.<String, Object>of("key", "labels", "label", "Labels",
                                   "type", "dynamic_dropdown", "resourceType", "labels",
                                   "dependsOn", List.of("repo"),
                                   "required", false,
                                   "helpText", "Optionally assign labels")
                        )
                    ),
                    Map.of(
                        "actionKey", "create-pr",
                        "name", "Create Pull Request",
                        "description", "Open a new pull request",
                        "configSchema", List.of(
                            repoField,
                            Map.of("key", "title", "label", "PR Title",
                                   "type", "text", "required", true,
                                   "placeholder", "Feature: New login page",
                                   "helpText", "Title for the pull request"),
                            Map.<String, Object>of("key", "head", "label", "Head Branch",
                                   "type", "dynamic_dropdown", "resourceType", "branches",
                                   "dependsOn", List.of("repo"),
                                   "required", true,
                                   "helpText", "The branch with your changes"),
                            Map.<String, Object>of("key", "base", "label", "Base Branch",
                                   "type", "dynamic_dropdown", "resourceType", "branches",
                                   "dependsOn", List.of("repo"),
                                   "required", true,
                                   "helpText", "The branch to merge into (e.g. main)"),
                            Map.of("key", "body", "label", "PR Description",
                                   "type", "textarea", "required", false,
                                   "helpText", "Pull request body in Markdown")
                        )
                    ),
                    Map.of(
                        "actionKey", "close-issue",
                        "name", "Close Issue",
                        "description", "Close an open issue",
                        "configSchema", List.of(
                            repoField,
                            Map.<String, Object>of("key", "issueNumber", "label", "Issue",
                                   "type", "dynamic_dropdown", "resourceType", "issues",
                                   "dependsOn", List.of("repo"),
                                   "required", true,
                                   "helpText", "Select the issue to close")
                        )
                    ),
                    Map.of(
                        "actionKey", "add-comment",
                        "name", "Add Comment to Issue",
                        "description", "Post a comment on an issue or pull request",
                        "configSchema", List.of(
                            repoField,
                            Map.<String, Object>of("key", "issueNumber", "label", "Issue / PR",
                                   "type", "dynamic_dropdown", "resourceType", "issues",
                                   "dependsOn", List.of("repo"),
                                   "required", true,
                                   "helpText", "Select the issue or PR to comment on"),
                            Map.of("key", "body", "label", "Comment Body",
                                   "type", "textarea", "required", true,
                                   "placeholder", "LGTM!",
                                   "helpText", "Comment body in Markdown")
                        )
                    )
                )
        )
        .credentialSchema(List.of(
            Map.of("key", "apiKey", "label", "Personal Access Token",
                    "type", "password", "required", true,
                    "placeholder", "github_pat_...",
                    "helpText", "Create a fine-grained PAT at github.com/settings/tokens?type=beta",
                    "authOption", "APIKEY")
        ))
        .altAuthType(AuthType.APIKEY)
        .category("developer")
        .helpUrl("https://github.com/settings/developers");
    }
}
