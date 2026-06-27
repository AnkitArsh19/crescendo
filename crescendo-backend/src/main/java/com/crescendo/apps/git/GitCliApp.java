package com.crescendo.apps.git;

import com.crescendo.app.App;
import com.crescendo.apps.AppDefinition;
import com.crescendo.enums.AuthType;
import org.springframework.stereotype.Component;
import java.util.*;

@Component
public class GitCliApp implements AppDefinition {
    public App toApp() {
        return new App(
                "git",
                "Git", """
                Git is the world's most popular distributed version control system. The Crescendo Git app allows you to safely run local repository commands to automate development workflows.

                **What you can do with Git in Crescendo:**
                - Automatically run `git pull` on a production server when a webhook fires from a successful build
                - Fetch `git status` daily and alert your team in Slack if there are uncommitted changes on a shared machine
                - Run a `git log` query and format the recent commit messages into a release notes document
                - Create automated backup scripts that pull specific repositories nightly

                **Actions available:**
                - Status — run `git status` to check the working tree
                - Pull — run `git pull` to fetch and integrate changes
                - Log — run `git log` to show commit history

                **Who should use this:** DevOps engineers, backend developers, and sysadmins managing local repositories and deployment scripts.

                **Authentication:** None required (runs commands on the local machine where the worker is hosted).
                """,
                "/icons/git.svg",
                AuthType.NONE,
                List.of(),
                List.of(
                        Map.of(
                                "actionKey", "status",
                                "name", "Status",
                                "description", "Run git status",
                                "configSchema", List.of(
                                        Map.of("key", "repoPath", "label", "Repository Path", "type", "text", "required", true)
                                )
                        ),
                        Map.of(
                                "actionKey", "pull",
                                "name", "Pull",
                                "description", "Run git pull",
                                "configSchema", List.of(
                                        Map.of("key", "repoPath", "label", "Repository Path", "type", "text", "required", true)
                                )
                        ),
                        Map.of(
                                "actionKey", "log",
                                "name", "Log",
                                "description", "Show recent commits",
                                "configSchema", List.of(
                                        Map.of("key", "repoPath", "label", "Repository Path", "type", "text", "required", true),
                                        Map.of("key", "limit", "label", "Limit", "type", "text", "required", false, "placeholder", "5")
                                )
                        )
                )
        ).credentialSchema(List.of()).category("developer").helpUrl("https://git-scm.com/docs");
    }
}
