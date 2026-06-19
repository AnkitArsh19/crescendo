package com.crescendo.apps.calendly;

import com.crescendo.apps.simpleapi.SimpleApiSupport;
import com.crescendo.execution.action.*;
import tools.jackson.databind.ObjectMapper;
import java.util.*;

@ActionMapping(appKey = "calendly", actionKey = "get-current-user")
class CalendlyGetCurrentUserHandler implements ActionHandler {
    private final ObjectMapper m;

    CalendlyGetCurrentUserHandler(ObjectMapper m) {
        this.m = m;
    }

    @Override
    public ActionResult execute(ActionContext c) {
        try {
            return SimpleApiSupport.parsed(
                    m,
                    SimpleApiSupport.bearer("https://api.calendly.com", SimpleApiSupport.cred(c, "accessToken"))
                            .get()
                            .uri("/users/me")
                            .retrieve()
                            .body(String.class)
            );
        } catch (Exception e) {
            return ActionResult.failure("Calendly current user failed: " + e.getMessage());
        }
    }
}

@ActionMapping(appKey = "calendly", actionKey = "list-events")
class CalendlyListEventsHandler implements ActionHandler {
    private final ObjectMapper m;

    CalendlyListEventsHandler(ObjectMapper m) {
        this.m = m;
    }

    @Override
    public ActionResult execute(ActionContext c) {
        try {
            String uri = "/scheduled_events?count={count}";
            List<Object> vars = new ArrayList<>();
            vars.add(Math.max(1, SimpleApiSupport.intCfg(c, "count", 20)));
            if (!SimpleApiSupport.cfg(c, "userUri").isBlank()) {
                uri += "&user={user}";
                vars.add(SimpleApiSupport.cfg(c, "userUri"));
            }
            if (!SimpleApiSupport.cfg(c, "organizationUri").isBlank()) {
                uri += "&organization={org}";
                vars.add(SimpleApiSupport.cfg(c, "organizationUri"));
            }
            String res = SimpleApiSupport.bearer("https://api.calendly.com", SimpleApiSupport.cred(c, "accessToken"))
                    .get()
                    .uri(uri, vars.toArray())
                    .retrieve()
                    .body(String.class);
            return SimpleApiSupport.parsed(m, res);
        } catch (Exception e) {
            return ActionResult.failure("Calendly list events failed: " + e.getMessage());
        }
    }
}
