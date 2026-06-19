package com.crescendo.apps.hubspot;

import com.crescendo.apps.simpleapi.SimpleApiSupport;
import com.crescendo.execution.action.*;
import tools.jackson.databind.ObjectMapper;
import org.springframework.http.MediaType;
import java.util.*;

@ActionMapping(appKey = "hubspot", actionKey = "search-contacts")
public class HubSpotSearchContactsHandler implements ActionHandler {
    private final ObjectMapper mapper;

    public HubSpotSearchContactsHandler(ObjectMapper m) {
        mapper = m;
    }

    @Override
    public ActionResult execute(ActionContext c) {
        try {
            String email = SimpleApiSupport.cfg(c, "email");
            String res = SimpleApiSupport.bearer("https://api.hubapi.com", SimpleApiSupport.cred(c, "accessToken"))
                    .post()
                    .uri("/crm/v3/objects/contacts/search")
                    .contentType(MediaType.APPLICATION_JSON)
                    .body(Map.of(
                            "filterGroups", List.of(
                                    Map.of("filters", List.of(
                                            Map.of(
                                                    "propertyName", "email",
                                                    "operator", "EQ",
                                                    "value", email
                                            )
                                    ))
                            )
                    ))
                    .retrieve()
                    .body(String.class);
            return SimpleApiSupport.parsed(mapper, res);
        } catch (Exception e) {
            return ActionResult.failure("HubSpot search contacts failed: " + e.getMessage());
        }
    }
}
