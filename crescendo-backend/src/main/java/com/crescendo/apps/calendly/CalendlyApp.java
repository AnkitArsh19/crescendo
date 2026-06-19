package com.crescendo.apps.calendly;

import com.crescendo.app.App;
import com.crescendo.apps.AppDefinition;
import com.crescendo.enums.AuthType;
import org.springframework.stereotype.Component;
import java.util.*;

@Component
public class CalendlyApp implements AppDefinition {
    public App toApp() {
        return new App(
                "calendly",
                "Calendly",
                "Read Calendly users and scheduled events",
                "https://www.google.com/s2/favicons?domain=calendly.com&sz=128",
                AuthType.OAUTH2,
                List.of(
                        Map.of(
                                "triggerKey", "event-created",
                                "name", "Event Created",
                                "description", "Triggers from Calendly webhook event.created",
                                "configSchema", List.of()
                        ),
                        Map.of(
                                "triggerKey", "event-canceled",
                                "name", "Event Canceled",
                                "description", "Triggers from Calendly webhook event.canceled",
                                "configSchema", List.of()
                        )
                ),
                List.of(
                        Map.of(
                                "actionKey", "list-events",
                                "name", "List Scheduled Events",
                                "description", "List scheduled events",
                                "configSchema", List.of(
                                        Map.of("key", "userUri", "label", "User URI", "type", "text", "required", false),
                                        Map.of("key", "organizationUri", "label", "Organization URI", "type", "text", "required", false),
                                        Map.of("key", "count", "label", "Count", "type", "text", "required", false, "placeholder", "20")
                                )
                        ),
                        Map.of(
                                "actionKey", "get-current-user",
                                "name", "Get Current User",
                                "description", "Fetch authenticated user",
                                "configSchema", List.of()
                        )
                )
        ).credentialSchema(List.of()).altAuthType(AuthType.OAUTH2).category("productivity").helpUrl("https://developer.calendly.com/");
    }
}
