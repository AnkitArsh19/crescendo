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
        return new App("leetcode", "LeetCode", "Fetch your LeetCode stats and recent submissions",
                "/icons/leetcode.svg", AuthType.NONE,
                List.of(),
                List.of(
                    Map.of(
                        "actionKey", "get-user-stats",
                        "name", "Get User Stats",
                        "description", "Retrieve LeetCode profile stats for a username",
                        "configSchema", List.of(
                            Map.of("key", "username", "label", "LeetCode Username",
                                   "type", "text", "required", true,
                                   "placeholder", "leetcoder123",
                                   "helpText", "The LeetCode username to fetch stats for")
                        )
                    ),
                    Map.of(
                        "actionKey", "get-daily-problem",
                        "name", "Get Daily Problem",
                        "description", "Get today's LeetCode Daily Challenge problem"
                    )
                )
        )
        .credentialSchema(List.of())
        .category("developer")
        .helpUrl("https://leetcode.com/");
    }
}
