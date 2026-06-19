package com.crescendo.apps.brandfetch;

import com.crescendo.apps.simpleapi.SimpleApiSupport;
import com.crescendo.execution.action.*;
import tools.jackson.databind.ObjectMapper;
import org.springframework.http.HttpHeaders;
import org.springframework.web.client.RestClient;

@ActionMapping(appKey = "brandfetch", actionKey = "get-brand")
public class BrandfetchGetBrandHandler implements ActionHandler {
    private final ObjectMapper mapper;

    public BrandfetchGetBrandHandler(ObjectMapper m) {
        mapper = m;
    }

    @Override
    public ActionResult execute(ActionContext c) {
        try {
            String res = RestClient.builder()
                    .baseUrl("https://api.brandfetch.io/v2")
                    .defaultHeader(HttpHeaders.AUTHORIZATION, "Bearer " + SimpleApiSupport.cred(c, "apiKey"))
                    .build()
                    .get()
                    .uri("/brands/{domain}", SimpleApiSupport.cfg(c, "domain"))
                    .retrieve()
                    .body(String.class);
            return SimpleApiSupport.parsed(mapper, res);
        } catch (Exception e) {
            return ActionResult.failure("Brandfetch get brand failed: " + e.getMessage());
        }
    }
}
