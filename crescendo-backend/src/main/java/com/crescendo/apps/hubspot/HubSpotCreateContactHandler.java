package com.crescendo.apps.hubspot;

import com.crescendo.apps.simpleapi.SimpleApiSupport;
import com.crescendo.execution.action.*;
import tools.jackson.databind.ObjectMapper;
import org.springframework.http.MediaType;
import java.util.*;

@ActionMapping(appKey = "hubspot", actionKey = "create-contact")
public class HubSpotCreateContactHandler implements ActionHandler {
    private final ObjectMapper mapper;

    public HubSpotCreateContactHandler(ObjectMapper m) {
        mapper = m;
    }

    @Override
    public ActionResult execute(ActionContext c) {
        try {
            String email = SimpleApiSupport.cfg(c, "email");
            if (email.isBlank()) {
                return ActionResult.failure("HubSpot email is required");
            }
            Map<String, Object> props = new LinkedHashMap<>();
            props.put("email", email);
            if (!SimpleApiSupport.cfg(c, "firstName").isBlank()) {
                props.put("firstname", SimpleApiSupport.cfg(c, "firstName"));
            }
            if (!SimpleApiSupport.cfg(c, "lastName").isBlank()) {
                props.put("lastname", SimpleApiSupport.cfg(c, "lastName"));
            }
            String res = SimpleApiSupport.bearer("https://api.hubapi.com", SimpleApiSupport.cred(c, "accessToken"))
                    .post()
                    .uri("/crm/v3/objects/contacts")
                    .contentType(MediaType.APPLICATION_JSON)
                    .body(Map.of("properties", props))
                    .retrieve()
                    .body(String.class);
            return SimpleApiSupport.parsed(mapper, res);
        } catch (Exception e) {
            return ActionResult.failure("HubSpot create contact failed: " + e.getMessage());
        }
    }
}
