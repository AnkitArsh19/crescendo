package com.crescendo.apps.calcom;

import com.crescendo.app.App;
import com.crescendo.apps.AppDefinition;
import com.crescendo.enums.AuthType;
import org.springframework.stereotype.Component;
import java.util.*;

@Component
public class CalComApp implements AppDefinition {
    public App toApp() {
        return new App(
                "cal-com",
                "Cal.com",
                "Read Cal.com bookings and event types",
                "/icons/calcom.svg",
                AuthType.APIKEY,
                List.of(
                        Map.of(
                                "triggerKey", "booking-created",
                                "name", "Booking Created",
                                "description", "Triggers from Cal.com webhook",
                                "configSchema", List.of()
                        ),
                        Map.of(
                                "triggerKey", "booking-cancelled",
                                "name", "Booking Cancelled",
                                "description", "Triggers from Cal.com webhook",
                                "configSchema", List.of()
                        )
                ),
                List.of(
                        Map.of(
                                "actionKey", "list-bookings",
                                "name", "List Bookings",
                                "description", "List bookings",
                                "configSchema", List.of()
                        ),
                        Map.of(
                                "actionKey", "list-event-types",
                                "name", "List Event Types",
                                "description", "List event types",
                                "configSchema", List.of()
                        )
                )
        ).credentialSchema(List.of(
                Map.of("key", "apiKey", "label", "API Key", "type", "password", "required", true),
                Map.of("key", "baseUrl", "label", "Base URL", "type", "text", "required", false, "placeholder", "https://api.cal.com/v1")
        )).category("productivity").helpUrl("https://cal.com/docs/api-reference/v1/introduction");
    }
}
