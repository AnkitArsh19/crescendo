package com.crescendo.apps.quotes;

import com.crescendo.execution.action.ActionContext;
import com.crescendo.execution.action.ActionHandler;
import com.crescendo.execution.action.ActionMapping;
import com.crescendo.execution.action.ActionResult;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.web.client.RestClient;

import java.util.HashMap;
import java.util.Map;

@ActionMapping(appKey = "quotes", actionKey = "get-by-category")
public class QuotesByCategoryHandler implements ActionHandler {

    private static final Logger logger = LoggerFactory.getLogger(QuotesByCategoryHandler.class);
    private static final String QUOTES_API = "https://zenquotes.io/api";

    @Override
    public ActionResult execute(ActionContext context) {
        Map<String, Object> config = context.configuration();

        String category = config.get("category") != null ? config.get("category").toString() : null;
        if (category == null || category.isBlank()) {
            return ActionResult.failure("'category' is required");
        }

        try {
            String response = RestClient.create()
                    .get()
                    .uri(QUOTES_API + "/random?tags={tag}", category)
                    .retrieve()
                    .body(String.class);

            Map<String, Object> output = new HashMap<>();
            output.put("response", response);
            logger.info("[quotes] Quote fetched for category={}", category);
            return ActionResult.success(output);
        } catch (Exception e) {
            logger.error("[quotes] Category quote failed", e);
            return ActionResult.failure("Failed to fetch quote by category: " + e.getMessage());
        }
    }
}
