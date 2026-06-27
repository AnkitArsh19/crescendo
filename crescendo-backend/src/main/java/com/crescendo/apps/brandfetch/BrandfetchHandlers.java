package com.crescendo.apps.brandfetch;

import com.crescendo.execution.action.ActionContext;
import com.crescendo.execution.action.ActionMapping;
import org.springframework.stereotype.Component;

import com.crescendo.execution.action.ActionResult;
import org.springframework.web.client.RestClient;
import java.util.Map;

@Component
public class BrandfetchHandlers {

    @ActionMapping(appKey = "brandfetch", actionKey = "get-brand")
    public Object getBrand(ActionContext context) throws Exception {
        String domain = context.configuration().get("domain") != null ? context.configuration().get("domain").toString() : "";
        if (domain.isBlank()) {
            return ActionResult.failure("Brandfetch domain is required");
        }

        try {
            String response = RestClient.create("https://api.brandfetch.io/v2")
                    .get()
                    .uri("/brands/{domain}", domain)
                    .retrieve()
                    .body(String.class);
            return ActionResult.success(Map.of("data", response));
        } catch (Exception e) {
            return ActionResult.failure("Failed to fetch brand profile: " + e.getMessage());
        }
    }
}
