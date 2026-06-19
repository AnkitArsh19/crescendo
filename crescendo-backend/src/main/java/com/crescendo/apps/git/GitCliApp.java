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
                "Git",
                "Run safe Git commands on local repositories",
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
