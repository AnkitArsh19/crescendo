package com.crescendo.apps.catfacts;

import com.crescendo.execution.action.*;
import org.springframework.web.client.RestClient;
import java.util.*;

/**
 * Gets multiple cat facts via catfact.ninja/facts.
 */
@ActionMapping(appKey = "cat-facts", actionKey = "get-multiple-facts")
public class CatFactsGetMultipleHandler implements ActionHandler {
    private final RestClient restClient = RestClient.create();

    @Override
    @SuppressWarnings("unchecked")
    public ActionResult execute(ActionContext context) {
        Map<String, Object> config = context.configuration();
        String limit = config.getOrDefault("limit", "5").toString();

        try {
            Map<String, Object> resp = restClient.get()
                    .uri("https://catfact.ninja/facts?limit=" + limit)
                    .retrieve().body(Map.class);

            Map<String, Object> out = new HashMap<>();
            out.put("provider", "cat-facts");
            out.put("action", "get-multiple-facts");
            out.put("facts", resp != null ? resp.get("data") : null);
            return ActionResult.success(out);
        } catch (Exception e) {
            return ActionResult.failure("Cat Facts failed: " + e.getMessage());
        }
    }
}
