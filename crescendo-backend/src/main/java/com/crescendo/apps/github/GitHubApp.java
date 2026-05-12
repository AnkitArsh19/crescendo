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
                "required", true, "helpText", "Select the repository");
        var branchField = Map.<String, Object>of("key", "branch", "label", "Branch",
                "type", "dynamic_dropdown", "resourceType", "branches",
                "dependsOn", List.of("repo"), "required", false,
                "helpText", "Optionally filter to a specific branch");
        var issueField = Map.<String, Object>of("key", "issueNumber", "label", "Issue / PR",
                "type", "dynamic_dropdown", "resourceType", "issues",
                "dependsOn", List.of("repo"), "required", true,
                "helpText", "Select the issue or PR");

        return new App("github", "GitHub", "Create issues, PRs, releases and manage repositories",
                "/icons/github.svg", AuthType.OAUTH2,
                List.of(
                    Map.of("triggerKey", "new-issue", "name", "New Issue",
                        "description", "Triggers when a new issue is created",
                        "configSchema", List.of(repoField)),
                    Map.of("triggerKey", "new-pr", "name", "New Pull Request",
                        "description", "Triggers when a new PR is opened",
                        "configSchema", List.of(repoField)),
                    Map.of("triggerKey", "push", "name", "New Commit/Push",
                        "description", "Triggers on push to repository",
                        "configSchema", List.of(repoField, branchField)),
                    Map.of("triggerKey", "new-release", "name", "New Release",
                        "description", "Triggers when a release is published",
                        "configSchema", List.of(repoField)),
                    Map.of("triggerKey", "new-star", "name", "New Star",
                        "description", "Triggers when someone stars the repository",
                        "configSchema", List.of(repoField))
                ),
                List.of(
                    Map.of("actionKey", "create-issue", "name", "Create Issue",
                        "description", "Open a new issue",
                        "configSchema", List.of(repoField,
                            Map.of("key", "title", "label", "Title", "type", "text", "required", true,
                                   "placeholder", "Bug: ...", "helpText", "Issue title"),
                            Map.of("key", "body", "label", "Body", "type", "textarea", "required", false,
                                   "helpText", "Issue body (Markdown)"),
                            Map.<String, Object>of("key", "labels", "label", "Labels",
                                   "type", "dynamic_dropdown", "resourceType", "labels",
                                   "dependsOn", List.of("repo"), "required", false, "helpText", "Labels"))),
                    Map.of("actionKey", "update-issue", "name", "Update Issue",
                        "description", "Modify an existing issue",
                        "configSchema", List.of(repoField, issueField,
                            Map.of("key", "title", "label", "Title", "type", "text", "required", false, "helpText", "New title"),
                            Map.of("key", "state", "label", "State", "type", "select", "required", false,
                                   "options", List.of(Map.of("value","open","label","Open"), Map.of("value","closed","label","Closed")),
                                   "helpText", "Open or close"))),
                    Map.of("actionKey", "create-pr", "name", "Create Pull Request",
                        "description", "Open a new pull request",
                        "configSchema", List.of(repoField,
                            Map.of("key", "title", "label", "Title", "type", "text", "required", true, "helpText", "PR title"),
                            Map.<String, Object>of("key", "head", "label", "Head Branch", "type", "dynamic_dropdown",
                                   "resourceType", "branches", "dependsOn", List.of("repo"), "required", true, "helpText", "Source branch"),
                            Map.<String, Object>of("key", "base", "label", "Base Branch", "type", "dynamic_dropdown",
                                   "resourceType", "branches", "dependsOn", List.of("repo"), "required", true, "helpText", "Target branch"),
                            Map.of("key", "body", "label", "Description", "type", "textarea", "required", false, "helpText", "PR body"))),
                    Map.of("actionKey", "add-comment", "name", "Add Comment",
                        "description", "Comment on an issue or PR",
                        "configSchema", List.of(repoField, issueField,
                            Map.of("key", "body", "label", "Comment", "type", "textarea", "required", true,
                                   "placeholder", "LGTM!", "helpText", "Comment body (Markdown)"))),
                    Map.of("actionKey", "close-issue", "name", "Close Issue",
                        "description", "Close an open issue",
                        "configSchema", List.of(repoField, issueField)),
                    Map.of("actionKey", "create-release", "name", "Create Release",
                        "description", "Publish a new release",
                        "configSchema", List.of(repoField,
                            Map.of("key", "tagName", "label", "Tag", "type", "text", "required", true,
                                   "placeholder", "v1.0.0", "helpText", "Git tag for the release"),
                            Map.of("key", "name", "label", "Release Name", "type", "text", "required", false, "helpText", "Title"),
                            Map.of("key", "body", "label", "Release Notes", "type", "textarea", "required", false, "helpText", "Markdown notes"),
                            Map.of("key", "draft", "label", "Draft?", "type", "dropdown", "required", false,
                                   "options", List.of(Map.of("value","false","label","No"), Map.of("value","true","label","Yes")), "helpText", "Create as draft"),
                            Map.of("key", "prerelease", "label", "Pre-release?", "type", "dropdown", "required", false,
                                   "options", List.of(Map.of("value","false","label","No"), Map.of("value","true","label","Yes")), "helpText", "Mark as pre-release")))
                )
        ).credentialSchema(List.of(
            Map.of("key", "apiKey", "label", "Personal Access Token", "type", "password", "required", true,
                    "placeholder", "github_pat_...", "helpText", "Create a PAT at github.com/settings/tokens", "authOption", "APIKEY")
        )).altAuthType(AuthType.APIKEY).category("developer").helpUrl("https://github.com/settings/developers");
    }
}
