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

@ActionMapping(appKey = "quotes", actionKey = "get-random-quote")
public class QuotesRandomHandler implements ActionHandler {

    private static final Logger logger = LoggerFactory.getLogger(QuotesRandomHandler.class);
    private static final String QUOTES_API = "https://zenquotes.io/api";

    @Override
    public ActionResult execute(ActionContext context) {
        try {
            String response = RestClient.create()
                    .get()
                    .uri(QUOTES_API + "/random")
                    .retrieve()
                    .body(String.class);

            Map<String, Object> output = new HashMap<>();
            output.put("response", response);
            logger.info("[quotes] Random quote fetched successfully");
            return ActionResult.success(output);
        } catch (Exception e) {
            logger.error("[quotes] Random quote failed", e);
            return ActionResult.failure("Failed to fetch random quote: " + e.getMessage());
        }
    }
}
