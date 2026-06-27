package com.crescendo.apps.leetcode;

import com.crescendo.app.App;
import com.crescendo.apps.AppDefinition;
import com.crescendo.enums.AuthType;
import org.springframework.stereotype.Component;
import java.util.List;
import java.util.Map;

@Component
public class LeetCodeApp implements AppDefinition {
    @Override
    public App toApp() {
        return new App("leetcode", "LeetCode", """
                LeetCode is a platform for enhancing coding skills and preparing for technical interviews. The Crescendo LeetCode app allows you to fetch user stats, daily challenges, and search for specific problems.

                **What you can do with LeetCode in Crescendo:**
                - Post the daily LeetCode challenge to a Discord study group every morning
                - Track your personal solving progress in a Google Sheet
                - Search for problems by topic and share them with your team
                - Set up automated reminders to practice specific algorithms

                **Actions available:**
                - Get User Profile — fetch a user's ranking and solved problem counts
                - Get Daily Challenge — retrieve today's featured problem
                - Search Problems — find problems by difficulty, tags, or keywords

                **Who should use this:** Software engineers preparing for interviews, coding bootcamps, and developer communities.

                **Authentication:** None required (accesses public GraphQL endpoints).
                """,
                "/icons/leetcode.svg", AuthType.NONE,
                List.of(),
                List.of(
                    Map.of("actionKey", "get-user-stats", "name", "Get User Stats",
                        "description", "Retrieve LeetCode profile stats",
                        "configSchema", List.of(
                            Map.of("key", "username", "label", "Username", "type", "text", "required", true,
                                   "placeholder", "leetcoder123", "helpText", "LeetCode username"))),
                    Map.of("actionKey", "get-daily-problem", "name", "Get Daily Challenge",
                        "description", "Get today's daily challenge problem",
                        "configSchema", List.of()),
                    Map.of("actionKey", "search-problem", "name", "Search Problems",
                        "description", "Search problems by keywords and difficulty",
                        "configSchema", List.of(
                            Map.of("key", "query", "label", "Search Query", "type", "text", "required", true,
                                   "placeholder", "two sum", "helpText", "Problem title or topic"),
                            Map.of("key", "difficulty", "label", "Difficulty", "type", "select", "required", false,
                                   "options", List.of(
                                       Map.of("value", "", "label", "Any"),
                                       Map.of("value", "EASY", "label", "Easy"),
                                       Map.of("value", "MEDIUM", "label", "Medium"),
                                       Map.of("value", "HARD", "label", "Hard")
                                   ), "helpText", "Filter by difficulty")))
                )
        ).credentialSchema(List.of()).category("developer").helpUrl("https://leetcode.com/");
    }
}
