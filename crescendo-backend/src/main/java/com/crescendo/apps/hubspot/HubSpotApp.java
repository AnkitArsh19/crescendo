package com.crescendo.apps.hubspot;

import com.crescendo.app.App;
import com.crescendo.apps.AppDefinition;
import com.crescendo.enums.AuthType;
import org.springframework.stereotype.Component;

import java.util.List;
import java.util.Map;

@Component
public class HubSpotApp implements AppDefinition {

    @Override
    public App toApp() {
        return new App(
                "hubspot",
                "HubSpot",
                """
                HubSpot is an inbound marketing and sales platform that helps companies attract visitors, convert leads, and close customers.
                
                This integration provides a comprehensive suite of operations mirroring the HubSpot REST API across standard objects:
                - **Company**: Create, Delete, Get, Get All, Get Recently Created/Updated, Search by Domain, Update
                - **Contact**: Upsert, Delete, Get, Get All, Get Recently Created/Updated, Search
                - **Contact List**: Add Contact, Remove Contact
                - **Deal**: Create, Delete, Get, Get All, Get Recently Created/Updated, Search, Update
                - **Engagement**: Create, Delete, Get, Get All
                - **Form**: Get Fields, Submit
                - **Ticket**: Create, Delete, Get, Get All, Update
                
                You can authenticate using an API Key, Service Key (App Token), or OAuth2.
                """,
                "https://www.google.com/s2/favicons?domain=hubspot.com&sz=128",
                AuthType.APIKEY,
                List.of(),
                List.of(
                        // COMPANY
                        Map.of("actionKey", "company-create", "name", "Create Company", "description", "Create a new company", "configSchema", List.of(Map.of("key", "name", "label", "Company Name", "type", "text", "required", true))),
                        Map.of("actionKey", "company-delete", "name", "Delete Company", "description", "Delete a company", "configSchema", List.of(Map.of("key", "companyId", "label", "Company", "type", "dynamic_dropdown", "resourceType", "companies", "required", true, "helpText", "Select company to delete"))),
                        Map.of("actionKey", "company-get", "name", "Get Company", "description", "Get a company", "configSchema", List.of(Map.of("key", "companyId", "label", "Company", "type", "dynamic_dropdown", "resourceType", "companies", "required", true, "helpText", "Select company to fetch"))),
                        Map.of("actionKey", "company-getAll", "name", "Get All Companies", "description", "Get many companies", "configSchema", List.of(Map.of("key", "limit", "label", "Limit", "type", "number", "default", 100))),
                        Map.of("actionKey", "company-getRecentlyCreatedUpdated", "name", "Get Recently Created/Updated Companies", "description", "Get recently created/updated companies", "configSchema", List.of(Map.of("key", "limit", "label", "Limit", "type", "number", "default", 100))),
                        Map.of("actionKey", "company-searchByDomain", "name", "Search Companies by Domain", "description", "Search companies by domain", "configSchema", List.of(Map.of("key", "domain", "label", "Domain", "type", "text", "required", true))),
                        Map.of("actionKey", "company-update", "name", "Update Company", "description", "Update a company", "configSchema", List.of(Map.of("key", "companyId", "label", "Company", "type", "dynamic_dropdown", "resourceType", "companies", "required", true, "helpText", "Select company to update"), Map.of("key", "properties", "label", "Properties (JSON)", "type", "json"))),

                        // CONTACT
                        Map.of("actionKey", "contact-upsert", "name", "Create or Update Contact", "description", "Create a new contact, or update the current one if it already exists (upsert)", "configSchema", List.of(Map.of("key", "email", "label", "Email", "type", "text", "required", true), Map.of("key", "properties", "label", "Properties (JSON)", "type", "json"))),
                        Map.of("actionKey", "contact-delete", "name", "Delete Contact", "description", "Delete a contact", "configSchema", List.of(Map.of("key", "contactId", "label", "Contact", "type", "dynamic_dropdown", "resourceType", "contacts", "required", true, "helpText", "Select contact to delete"))),
                        Map.of("actionKey", "contact-get", "name", "Get Contact", "description", "Get a contact", "configSchema", List.of(Map.of("key", "contactId", "label", "Contact", "type", "dynamic_dropdown", "resourceType", "contacts", "required", true, "helpText", "Select contact to fetch"))),
                        Map.of("actionKey", "contact-getAll", "name", "Get All Contacts", "description", "Get many contacts", "configSchema", List.of(Map.of("key", "limit", "label", "Limit", "type", "number", "default", 100))),
                        Map.of("actionKey", "contact-getRecentlyCreatedUpdated", "name", "Get Recently Created/Updated Contacts", "description", "Get recently created/updated contacts", "configSchema", List.of(Map.of("key", "limit", "label", "Limit", "type", "number", "default", 100))),
                        Map.of("actionKey", "contact-search", "name", "Search Contacts", "description", "Search contacts", "configSchema", List.of(Map.of("key", "query", "label", "Query", "type", "text", "required", true))),

                        // CONTACT LIST
                        Map.of("actionKey", "contactList-add", "name", "Add Contact to List", "description", "Add contact to a list", "configSchema", List.of(Map.of("key", "listId", "label", "Contact List", "type", "dynamic_dropdown", "resourceType", "lists", "required", true, "helpText", "Select HubSpot contact list"), Map.of("key", "emails", "label", "Emails (comma separated)", "type", "text"))),
                        Map.of("actionKey", "contactList-remove", "name", "Remove Contact from List", "description", "Remove contact from a list", "configSchema", List.of(Map.of("key", "listId", "label", "Contact List", "type", "dynamic_dropdown", "resourceType", "lists", "required", true, "helpText", "Select HubSpot contact list"), Map.of("key", "emails", "label", "Emails (comma separated)", "type", "text"))),

                        // DEAL
                        Map.of("actionKey", "deal-create", "name", "Create Deal", "description", "Create a deal", "configSchema", List.of(Map.of("key", "pipeline", "label", "Pipeline", "type", "dynamic_dropdown", "resourceType", "pipelines", "helpText", "Select deal pipeline"), Map.of("key", "stage", "label", "Stage", "type", "text"), Map.of("key", "properties", "label", "Properties (JSON)", "type", "json"))),
                        Map.of("actionKey", "deal-delete", "name", "Delete Deal", "description", "Delete a deal", "configSchema", List.of(Map.of("key", "dealId", "label", "Deal", "type", "dynamic_dropdown", "resourceType", "deals", "required", true, "helpText", "Select deal to delete"))),
                        Map.of("actionKey", "deal-get", "name", "Get Deal", "description", "Get a deal", "configSchema", List.of(Map.of("key", "dealId", "label", "Deal", "type", "dynamic_dropdown", "resourceType", "deals", "required", true, "helpText", "Select deal to fetch"))),
                        Map.of("actionKey", "deal-getAll", "name", "Get All Deals", "description", "Get many deals", "configSchema", List.of(Map.of("key", "limit", "label", "Limit", "type", "number", "default", 100))),
                        Map.of("actionKey", "deal-getRecentlyCreatedUpdated", "name", "Get Recently Created/Updated Deals", "description", "Get recently created/updated deals", "configSchema", List.of(Map.of("key", "limit", "label", "Limit", "type", "number", "default", 100))),
                        Map.of("actionKey", "deal-search", "name", "Search Deals", "description", "Search deals", "configSchema", List.of(Map.of("key", "query", "label", "Query", "type", "text", "required", true))),
                        Map.of("actionKey", "deal-update", "name", "Update Deal", "description", "Update a deal", "configSchema", List.of(Map.of("key", "dealId", "label", "Deal", "type", "dynamic_dropdown", "resourceType", "deals", "required", true, "helpText", "Select deal to update"), Map.of("key", "properties", "label", "Properties (JSON)", "type", "json"))),

                        // ENGAGEMENT
                        Map.of("actionKey", "engagement-create", "name", "Create Engagement", "description", "Create an engagement", "configSchema", List.of(Map.of("key", "type", "label", "Type (e.g. CALL, EMAIL, NOTE, MEETING, TASK)", "type", "text", "required", true), Map.of("key", "metadata", "label", "Metadata (JSON)", "type", "json"))),
                        Map.of("actionKey", "engagement-delete", "name", "Delete Engagement", "description", "Delete an engagement", "configSchema", List.of(Map.of("key", "engagementId", "label", "Engagement ID", "type", "text", "required", true))),
                        Map.of("actionKey", "engagement-get", "name", "Get Engagement", "description", "Get an engagement", "configSchema", List.of(Map.of("key", "engagementId", "label", "Engagement ID", "type", "text", "required", true))),
                        Map.of("actionKey", "engagement-getAll", "name", "Get All Engagements", "description", "Get many engagements", "configSchema", List.of(Map.of("key", "limit", "label", "Limit", "type", "number", "default", 100))),

                        // FORM
                        Map.of("actionKey", "form-getFields", "name", "Get Form Fields", "description", "Get all fields from a form", "configSchema", List.of(
                                Map.of("key", "formId", "label", "Form", "type", "dynamic_dropdown", "resourceType", "forms", "required", true, "helpText", "Select HubSpot form")
                        )),
                        Map.of("actionKey", "form-submit", "name", "Submit Form", "description", "Submit data to a HubSpot form", "configSchema", List.of(
                                Map.of("key", "formId", "label", "Form", "type", "dynamic_dropdown", "resourceType", "forms", "required", true, "helpText", "Select HubSpot form"),
                                Map.of("key", "portalId", "label", "Portal ID", "type", "text", "required", true),
                                Map.of("key", "fields", "label", "Fields (JSON object: fieldName -> value)", "type", "json"),
                                Map.of("key", "context", "label", "Context (JSON: hutk, pageUri, pageName, etc.)", "type", "json"),
                                Map.of("key", "skipValidation", "label", "Skip Validation", "type", "boolean", "default", false)
                        )),

                        // TICKET
                        Map.of("actionKey", "ticket-create", "name", "Create Ticket", "description", "Create a ticket", "configSchema", List.of(Map.of("key", "pipeline", "label", "Pipeline", "type", "text"), Map.of("key", "stage", "label", "Stage", "type", "text"), Map.of("key", "properties", "label", "Properties (JSON)", "type", "json"))),
                        Map.of("actionKey", "ticket-delete", "name", "Delete Ticket", "description", "Delete a ticket", "configSchema", List.of(Map.of("key", "ticketId", "label", "Ticket", "type", "dynamic_dropdown", "resourceType", "tickets", "required", true, "helpText", "Select ticket to delete"))),
                        Map.of("actionKey", "ticket-get", "name", "Get Ticket", "description", "Get a ticket", "configSchema", List.of(Map.of("key", "ticketId", "label", "Ticket", "type", "dynamic_dropdown", "resourceType", "tickets", "required", true, "helpText", "Select ticket to fetch"))),
                        Map.of("actionKey", "ticket-getAll", "name", "Get All Tickets", "description", "Get many tickets", "configSchema", List.of(Map.of("key", "limit", "label", "Limit", "type", "number", "default", 100))),
                        Map.of("actionKey", "ticket-update", "name", "Update Ticket", "description", "Update a ticket", "configSchema", List.of(Map.of("key", "ticketId", "label", "Ticket", "type", "dynamic_dropdown", "resourceType", "tickets", "required", true, "helpText", "Select ticket to update"), Map.of("key", "properties", "label", "Properties (JSON)", "type", "json")))
                )
        ).credentialSchema(List.of(
                Map.of("key", "apiKey", "label", "API Key / Access Token", "type", "password", "required", true)
        )).altAuthType(AuthType.OAUTH2).category("crm");
    }
}
