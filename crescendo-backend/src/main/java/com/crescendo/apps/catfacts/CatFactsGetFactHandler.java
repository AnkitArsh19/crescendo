package com.crescendo.apps.catfacts;

import com.crescendo.execution.action.ActionContext;
import com.crescendo.execution.action.ActionHandler;
import com.crescendo.execution.action.ActionMapping;
import com.crescendo.execution.action.ActionResult;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.web.client.RestClient;

import java.util.HashMap;
import java.util.Map;

@ActionMapping(appKey = "cat-facts", actionKey = "get-fact")
public class CatFactsGetFactHandler implements ActionHandler {

    private static final Logger logger = LoggerFactory.getLogger(CatFactsGetFactHandler.class);
    private static final String CATFACTS_API = "https://catfact.ninja/fact";

    @Override
    public ActionResult execute(ActionContext context) {
        try {
            String response = RestClient.create()
                    .get()
                    .uri(CATFACTS_API)
                    .retrieve()
                    .body(String.class);

            Map<String, Object> output = new HashMap<>();
            output.put("response", response);
            logger.info("[catfacts] Fact fetched successfully");
            return ActionResult.success(output);
        } catch (Exception e) {
            logger.error("[catfacts] Fetch fact failed", e);
            return ActionResult.failure("Failed to fetch cat fact: " + e.getMessage());
        }
    }
}
