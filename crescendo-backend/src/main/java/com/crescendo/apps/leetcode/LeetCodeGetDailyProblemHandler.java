package com.crescendo.apps.leetcode;

import com.crescendo.execution.action.ActionContext;
import com.crescendo.execution.action.ActionHandler;
import com.crescendo.execution.action.ActionMapping;
import com.crescendo.execution.action.ActionResult;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.http.MediaType;
import org.springframework.web.client.RestClient;

import java.util.HashMap;
import java.util.Map;

@ActionMapping(appKey = "leetcode", actionKey = "get-daily-problem")
public class LeetCodeGetDailyProblemHandler implements ActionHandler {

    private static final Logger logger = LoggerFactory.getLogger(LeetCodeGetDailyProblemHandler.class);
    private static final String LEETCODE_GRAPHQL = "https://leetcode.com/graphql";

    @Override
    public ActionResult execute(ActionContext context) {
        String query = """
            query questionOfToday {
              activeDailyCodingChallengeQuestion {
                date
                link
                question {
                  questionId
                  title
                  titleSlug
                  difficulty
                  topicTags {
                    name
                  }
                }
              }
            }
            """;

        try {
            String response = RestClient.create()
                    .post()
                    .uri(LEETCODE_GRAPHQL)
                    .contentType(MediaType.APPLICATION_JSON)
                    .body(Map.of("query", query))
                    .retrieve()
                    .body(String.class);

            Map<String, Object> output = new HashMap<>();
            output.put("response", response);
            logger.info("[leetcode] Daily problem fetched successfully");
            return ActionResult.success(output);
        } catch (Exception e) {
            logger.error("[leetcode] Get daily problem failed", e);
            return ActionResult.failure("LeetCode get daily problem failed: " + e.getMessage());
        }
    }
}
