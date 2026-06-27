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
        return new App("github-stats", "GitHub Profile Stats", """
                The GitHub Profile Stats app provides a simple way to fetch aggregate public statistics for GitHub users, perfect for building dynamic portfolios or tracking developer activity.

                **What you can do with GitHub Profile Stats in Crescendo:**
                - Fetch a candidate's public repo count and language distribution during the hiring process
                - Update a personal portfolio website dynamically with your latest GitHub follower count
                - Send a weekly Slack summary of the most starred repositories for a specific user
                - Trigger a celebratory Discord message when a user hits a milestone number of repositories

                **Actions available:**
                - Get Profile — fetch basic user details like followers, following, and public repo count
                - Get Repositories — retrieve a list of public repositories for a user with star and fork counts

                **Who should use this:** Technical recruiters, developer relations managers, and developers building dynamic personal websites.

                **Authentication:** None required for public endpoints (API Key optional for higher limits).
                """,
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
