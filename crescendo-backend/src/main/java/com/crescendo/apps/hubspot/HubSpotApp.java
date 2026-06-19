package com.crescendo.apps.hubspot;

import com.crescendo.app.App;
import com.crescendo.apps.AppDefinition;
import com.crescendo.enums.AuthType;
import org.springframework.stereotype.Component;
import java.util.*;

@Component
public class HubSpotApp implements AppDefinition {
    public App toApp() {
        return new App(
                "hubspot",
                "HubSpot",
                "Create and search CRM contacts",
                "https://www.google.com/s2/favicons?domain=hubspot.com&sz=128",
                AuthType.OAUTH2,
                List.of(),
                List.of(
                        Map.of(
                                "actionKey", "create-contact",
                                "name", "Create Contact",
                                "description", "Create a CRM contact",
                                "configSchema", List.of(
                                        Map.of("key", "email", "label", "Email", "type", "text", "required", true),
                                        Map.of("key", "firstName", "label", "First Name", "type", "text", "required", false),
                                        Map.of("key", "lastName", "label", "Last Name", "type", "text", "required", false)
                                )
                        ),
                        Map.of(
                                "actionKey", "search-contacts",
                                "name", "Search Contacts",
                                "description", "Search contacts by email",
                                "configSchema", List.of(
                                        Map.of("key", "email", "label", "Email", "type", "text", "required", true)
                                )
                        )
                )
        ).credentialSchema(List.of()).category("crm").helpUrl("https://developers.hubspot.com/docs/api/crm/contacts");
    }
}
