package com.crescendo.apps.freshdesk;

import com.crescendo.app.App;
import com.crescendo.apps.AppDefinition;
import com.crescendo.enums.AuthType;
import org.springframework.stereotype.Component;

import java.util.List;
import java.util.Map;

@Component
public class FreshdeskApp implements AppDefinition {

    @Override
    public App toApp() {
        return new App("freshdesk", "Freshdesk", "Create and fetch Freshdesk tickets",
                "/icons/freshdesk.svg", AuthType.APIKEY,
                List.of(),
                List.of(
                        Map.of("actionKey", "create-ticket", "name", "Create Ticket",
                                "description", "Create a Freshdesk support ticket",
                                "configSchema", List.of(
                                        Map.of("key", "email", "label", "Requester Email", "type", "text", "required", true),
                                        Map.of("key", "subject", "label", "Subject", "type", "text", "required", true),
                                        Map.of("key", "description", "label", "Description", "type", "textarea", "required", true),
                                        Map.of("key", "priority", "label", "Priority", "type", "text", "required", false,
                                                "placeholder", "1"),
                                        Map.of("key", "status", "label", "Status", "type", "text", "required", false,
                                                "placeholder", "2"))),
                        Map.of("actionKey", "get-ticket", "name", "Get Ticket",
                                "description", "Fetch a Freshdesk ticket by ID",
                                "configSchema", List.of(
                                        Map.of("key", "ticketId", "label", "Ticket ID", "type", "text", "required", true)))
                )
        ).credentialSchema(List.of(
                Map.of("key", "domain", "label", "Domain", "type", "text", "required", true,
                        "placeholder", "yourcompany.freshdesk.com"),
                Map.of("key", "apiKey", "label", "API Key", "type", "password", "required", true)
        )).category("support").helpUrl("https://developers.freshdesk.com/api/");
    }
}
