package com.crescendo.apps.linkedin;

import com.crescendo.execution.action.ActionContext;
import com.crescendo.execution.action.ActionHandler;
import com.crescendo.execution.action.ActionMapping;
import com.crescendo.execution.action.ActionResult;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.http.HttpHeaders;
import org.springframework.web.client.RestClient;

import java.util.HashMap;
import java.util.Map;

@ActionMapping(appKey = "linkedin", actionKey = "get-profile")
public class LinkedInGetProfileHandler implements ActionHandler {

    private static final Logger logger = LoggerFactory.getLogger(LinkedInGetProfileHandler.class);
    private static final String LINKEDIN_API = "https://api.linkedin.com/v2";

    @Override
    public ActionResult execute(ActionContext context) {
        Map<String, Object> creds = context.credentials();

        String token = creds != null ? (String) creds.get("accessToken") : null;
        if (token == null || token.isBlank()) return ActionResult.failure("LinkedIn requires 'accessToken'");

        try {
            String response = RestClient.create()
                    .get()
                    .uri(LINKEDIN_API + "/me?projection=(id,firstName,lastName,profilePicture)")
                    .header(HttpHeaders.AUTHORIZATION, "Bearer " + token)
                    .retrieve()
                    .body(String.class);

            Map<String, Object> output = new HashMap<>();
            output.put("response", response);
            logger.info("[linkedin] Profile fetched successfully");
            return ActionResult.success(output);
        } catch (Exception e) {
            logger.error("[linkedin] Get profile failed", e);
            return ActionResult.failure("LinkedIn get profile failed: " + e.getMessage());
        }
    }
}
