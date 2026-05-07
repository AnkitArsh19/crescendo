package com.crescendo.apps.githubstats;

import com.crescendo.app.App;
import com.crescendo.apps.AppDefinition;
import com.crescendo.enums.AuthType;
import org.springframework.stereotype.Component;

import java.util.List;
import java.util.Map;

@Component
public class GitHubProfileStatsApp implements AppDefinition {

    @Override
    public App toApp() {
        return new App("github-stats", "GitHub Profile Stats", "Fetch public GitHub profile statistics",
                "/icons/github.svg", AuthType.NONE,
                List.of(),
                List.of(
                    Map.of(
                        "actionKey", "get-profile-stats",
                        "name", "Get Profile Stats",
                        "description", "Retrieve public profile stats (repos, followers, contributions)",
                        "configSchema", List.of(
                            Map.of("key", "username", "label", "GitHub Username",
                                   "type", "text", "required", true,
                                   "placeholder", "octocat",
                                   "helpText", "The GitHub username to fetch stats for")
                        )
                    ),
                    Map.of(
                        "actionKey", "get-repos",
                        "name", "List Public Repos",
                        "description", "List public repositories for a GitHub user",
                        "configSchema", List.of(
                            Map.of("key", "username", "label", "GitHub Username",
                                   "type", "text", "required", true,
                                   "placeholder", "octocat",
                                   "helpText", "The GitHub username to list repos for"),
                            Map.of("key", "sort", "label", "Sort By",
                                   "type", "select", "required", false,
                                   "options", List.of(
                                       Map.of("value", "updated", "label", "Recently Updated"),
                                       Map.of("value", "stars", "label", "Most Stars"),
                                       Map.of("value", "created", "label", "Recently Created")
                                   ),
                                   "helpText", "Sort order for repositories")
                        )
                    )
                )
        )
        .credentialSchema(List.of())
        .category("developer")
        .helpUrl("https://github.com/");
    }
}
