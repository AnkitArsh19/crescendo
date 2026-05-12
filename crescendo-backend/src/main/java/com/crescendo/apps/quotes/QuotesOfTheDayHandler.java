package com.crescendo.apps.quotes;

import com.crescendo.execution.action.*;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.web.client.RestClient;
import java.util.*;

/**
 * Gets the quote of the day from quotable.io API.
 */
@ActionMapping(appKey = "quotes", actionKey = "get-quote-of-day")
public class QuotesOfTheDayHandler implements ActionHandler {
    private static final Logger logger = LoggerFactory.getLogger(QuotesOfTheDayHandler.class);
    private final RestClient restClient = RestClient.create();

    @Override
    @SuppressWarnings("unchecked")
    public ActionResult execute(ActionContext context) {
        try {
            // quotable API — random quote as "quote of the day" proxy
            Map<String, Object> resp = restClient.get()
                    .uri("https://api.quotable.io/quotes/random?limit=1")
                    .retrieve().body(Map.class);

            Map<String, Object> out = new HashMap<>();
            out.put("provider", "quotes");
            out.put("action", "get-quote-of-day");
            if (resp != null) {
                out.put("quote", resp);
            }
            return ActionResult.success(out);
        } catch (Exception e) {
            // Fallback to zenquotes API
            try {
                String resp = restClient.get()
                        .uri("https://zenquotes.io/api/today")
                        .retrieve().body(String.class);
                Map<String, Object> out = new HashMap<>();
                out.put("provider", "quotes");
                out.put("action", "get-quote-of-day");
                out.put("raw", resp);
                return ActionResult.success(out);
            } catch (Exception e2) {
                return ActionResult.failure("Quotes failed: " + e2.getMessage());
            }
        }
    }
}
