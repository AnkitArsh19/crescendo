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
        return new App(
                "freshdesk",
                "Freshdesk",
                """
                Freshdesk is a cloud-based customer support software that helps businesses provide effortless customer service. 
                
                This integration supports managing:
                - **Contact**: Create, Delete, Get, Get All, Update
                - **Ticket**: Create, Delete, Get, Get All, Update
                
                Authenticate using an API Key. Note that you also need your Freshdesk domain.
                """,
                "https://www.google.com/s2/favicons?domain=freshdesk.com&sz=128",
                AuthType.APIKEY,
                List.of(),
                List.of(
                        // CONTACT
                        Map.of("actionKey", "freshdesk:contact:create", "name", "Create Contact", "description", "Create a contact in Freshdesk", "configSchema", List.of(Map.of("key", "name", "label", "Name", "type", "text", "required", true), Map.of("key", "email", "label", "Email", "type", "text", "required", true), Map.of("key", "payload", "label", "Additional JSON properties", "type", "json"))),
                        Map.of("actionKey", "freshdesk:contact:delete", "name", "Delete Contact", "description", "Delete a contact in Freshdesk", "configSchema", List.of(Map.of("key", "contactId", "label", "Contact ID", "type", "text", "required", true))),
                        Map.of("actionKey", "freshdesk:contact:get", "name", "Get Contact", "description", "Get a contact from Freshdesk", "configSchema", List.of(Map.of("key", "contactId", "label", "Contact ID", "type", "text", "required", true))),
                        Map.of("actionKey", "freshdesk:contact:getAll", "name", "Get All Contacts", "description", "Get all contacts from Freshdesk", "configSchema", List.of(Map.of("key", "limit", "label", "Limit", "type", "number", "default", 100))),
                        Map.of("actionKey", "freshdesk:contact:update", "name", "Update Contact", "description", "Update a contact in Freshdesk", "configSchema", List.of(Map.of("key", "contactId", "label", "Contact ID", "type", "text", "required", true), Map.of("key", "payload", "label", "JSON payload", "type", "json", "required", true))),

                        // TICKET
                        Map.of("actionKey", "freshdesk:ticket:create", "name", "Create Ticket", "description", "Create a ticket in Freshdesk", "configSchema", List.of(Map.of("key", "email", "label", "Email", "type", "text"), Map.of("key", "subject", "label", "Subject", "type", "text"), Map.of("key", "description", "label", "Description", "type", "text"), Map.of("key", "status", "label", "Status", "type", "number"), Map.of("key", "priority", "label", "Priority", "type", "number"), Map.of("key", "payload", "label", "Additional JSON properties", "type", "json"))),
                        Map.of("actionKey", "freshdesk:ticket:delete", "name", "Delete Ticket", "description", "Delete a ticket in Freshdesk", "configSchema", List.of(Map.of("key", "ticketId", "label", "Ticket ID", "type", "text", "required", true))),
                        Map.of("actionKey", "freshdesk:ticket:get", "name", "Get Ticket", "description", "Get a ticket from Freshdesk", "configSchema", List.of(Map.of("key", "ticketId", "label", "Ticket ID", "type", "text", "required", true))),
                        Map.of("actionKey", "freshdesk:ticket:getAll", "name", "Get All Tickets", "description", "Get all tickets from Freshdesk", "configSchema", List.of(Map.of("key", "limit", "label", "Limit", "type", "number", "default", 100))),
                        Map.of("actionKey", "freshdesk:ticket:update", "name", "Update Ticket", "description", "Update a ticket in Freshdesk", "configSchema", List.of(Map.of("key", "ticketId", "label", "Ticket ID", "type", "text", "required", true), Map.of("key", "payload", "label", "JSON payload", "type", "json", "required", true)))
                )
        ).credentialSchema(List.of(
                Map.of("key", "domain", "label", "Freshdesk Domain", "type", "text", "required", true, "placeholder", "yourdomain.freshdesk.com"),
                Map.of("key", "apiKey", "label", "API Key", "type", "password", "required", true)
        )).category("support");
    }
}
