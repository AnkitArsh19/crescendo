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

@ActionMapping(appKey = "leetcode", actionKey = "get-user-stats")
public class LeetCodeGetUserProfileHandler implements ActionHandler {

  private static final Logger logger = LoggerFactory.getLogger(LeetCodeGetUserProfileHandler.class);
  private static final String LEETCODE_GRAPHQL = "https://leetcode.com/graphql";

  @Override
  public ActionResult execute(ActionContext context) {
    Map<String, Object> config = context.configuration();

    String username = config.get("username") != null ? config.get("username").toString() : null;
    if (username == null || username.isBlank())
      return ActionResult.failure("'username' is required");

    String query = """
        query getUserProfile($username: String!) {
          matchedUser(username: $username) {
            username
            profile {
              realName
              ranking
              reputation
              starRating
            }
            submitStats {
              acSubmissionNum {
                difficulty
                count
              }
            }
          }
        }
        """;

    try {
      Map<String, Object> body = Map.of(
          "query", query,
          "variables", Map.of("username", username));

      String response = RestClient.create()
          .post()
          .uri(LEETCODE_GRAPHQL)
          .contentType(MediaType.APPLICATION_JSON)
          .body(body)
          .retrieve()
          .body(String.class);

      Map<String, Object> output = new HashMap<>();
      output.put("response", response);
      output.put("username", username);
      logger.info("[leetcode] User profile fetched for user={}", username);
      return ActionResult.success(output);
    } catch (Exception e) {
      logger.error("[leetcode] Get user profile failed for {}", username, e);
      return ActionResult.failure("LeetCode get user profile failed: " + e.getMessage());
    }
  }
}
