package com.crescendo.apps.calcom;

import com.crescendo.apps.simpleapi.SimpleApiSupport;
import com.crescendo.execution.action.*;
import tools.jackson.databind.ObjectMapper;
import org.springframework.web.client.RestClient;

@ActionMapping(appKey = "cal-com", actionKey = "list-bookings")
class CalComListBookingsHandler implements ActionHandler {
    private final ObjectMapper m;

    CalComListBookingsHandler(ObjectMapper m) {
        this.m = m;
    }

    @Override
    public ActionResult execute(ActionContext c) {
        try {
            return SimpleApiSupport.parsed(
                    m,
                    client(c).get()
                            .uri("/bookings?apiKey={key}", SimpleApiSupport.cred(c, "apiKey"))
                            .retrieve()
                            .body(String.class)
            );
        } catch (Exception e) {
            return ActionResult.failure("Cal.com list bookings failed: " + e.getMessage());
        }
    }

    RestClient client(ActionContext c) {
        String b = SimpleApiSupport.cred(c, "baseUrl");
        return RestClient.create(b.isBlank() ? "https://api.cal.com/v1" : b);
    }
}

@ActionMapping(appKey = "cal-com", actionKey = "list-event-types")
class CalComListEventTypesHandler implements ActionHandler {
    private final ObjectMapper m;

    CalComListEventTypesHandler(ObjectMapper m) {
        this.m = m;
    }

    @Override
    public ActionResult execute(ActionContext c) {
        try {
            String b = SimpleApiSupport.cred(c, "baseUrl");
            return SimpleApiSupport.parsed(
                    m,
                    RestClient.create(b.isBlank() ? "https://api.cal.com/v1" : b).get()
                            .uri("/event-types?apiKey={key}", SimpleApiSupport.cred(c, "apiKey"))
                            .retrieve()
                            .body(String.class)
            );
        } catch (Exception e) {
            return ActionResult.failure("Cal.com list event types failed: " + e.getMessage());
        }
    }
}
