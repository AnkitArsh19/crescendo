package com.crescendo.apps.leetcode;

import com.crescendo.execution.action.*;
import org.springframework.http.MediaType;
import org.springframework.web.client.RestClient;
import java.util.*;

/**
 * Searches LeetCode problems via GraphQL using problemsetQuestionList.
 */
@ActionMapping(appKey = "leetcode", actionKey = "search-problem")
public class LeetCodeSearchProblemHandler implements ActionHandler {
    private final RestClient restClient = RestClient.create();

    @Override
public ActionResult execute(ActionContext context) {
        Map<String, Object> config = context.configuration();
        String query = config.get("query") != null ? config.get("query").toString() : null;
        if (query == null) return ActionResult.failure("'query' is required");
        String difficulty = config.get("difficulty") != null ? config.get("difficulty").toString() : null;

        try {
            String gql = "{\"query\":\"query problemsetQuestionList($categorySlug: String, $limit: Int, $skip: Int, $filters: QuestionListFilterInput) { problemsetQuestionList: questionList(categorySlug: $categorySlug, limit: $limit, skip: $skip, filters: $filters) { total: totalNum questions: data { questionId title titleSlug difficulty acRate } } }\","
                    + "\"variables\":{\"categorySlug\":\"\",\"skip\":0,\"limit\":10,\"filters\":{\"searchKeywords\":\"" + query + "\""
                    + (difficulty != null ? ",\"difficulty\":\"" + difficulty.toUpperCase() + "\"" : "")
                    + "}}}";

            String resp = restClient.post()
                    .uri("https://leetcode.com/graphql/")
                    .contentType(MediaType.APPLICATION_JSON)
                    .body(gql).retrieve().body(String.class);

            Map<String, Object> out = new HashMap<>();
            out.put("provider", "leetcode");
            out.put("action", "search-problem");
            out.put("query", query);
            out.put("response", resp);
            return ActionResult.success(out);
        } catch (Exception e) {
            return ActionResult.failure("LeetCode search failed: " + e.getMessage());
        }
    }
}
