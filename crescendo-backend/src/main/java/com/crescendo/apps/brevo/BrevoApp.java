package com.crescendo.apps.brevo;

import com.crescendo.app.App;
import com.crescendo.apps.AppDefinition;
import com.crescendo.enums.AuthType;
import org.springframework.stereotype.Component;

import java.util.List;
import java.util.Map;

@Component
public class BrevoApp implements AppDefinition {

    @Override
    public App toApp() {
        return new App(
                "brevo",
                "Brevo",
                """
                Brevo (formerly Sendinblue) is a SaaS solution for relationship marketing.
                
                This integration provides:
                - **Attribute**: Create, Update, Delete, Get All
                - **Contact**: Create, Upsert, Delete, Get, Get All, Update
                - **Email**: Send, Send Template
                - **Sender**: Create, Delete, Get All
                
                Authenticate using a Brevo API Key (v3).
                """,
                "/icons/brevo.svg",
                AuthType.APIKEY,
                List.of(),
                List.of(
                        // ATTRIBUTE
                        Map.of("actionKey", "brevo:attribute:create", "name", "Create Attribute", "description", "Create a contact attribute", "configSchema", List.of(Map.of("key", "category", "label", "Category", "type", "text"), Map.of("key", "name", "label", "Name", "type", "text", "required", true), Map.of("key", "type", "label", "Type", "type", "text"))),
                        Map.of("actionKey", "brevo:attribute:update", "name", "Update Attribute", "description", "Update a contact attribute", "configSchema", List.of(Map.of("key", "category", "label", "Category", "type", "text"), Map.of("key", "name", "label", "Name", "type", "text", "required", true), Map.of("key", "type", "label", "Type", "type", "text"))),
                        Map.of("actionKey", "brevo:attribute:delete", "name", "Delete Attribute", "description", "Delete a contact attribute", "configSchema", List.of(Map.of("key", "category", "label", "Category", "type", "text"), Map.of("key", "name", "label", "Name", "type", "text", "required", true))),
                        Map.of("actionKey", "brevo:attribute:getAll", "name", "Get All Attributes", "description", "Get all contact attributes", "configSchema", List.of()),

                        // CONTACT
                        Map.of("actionKey", "brevo:contact:create", "name", "Create Contact", "description", "Create a contact", "configSchema", List.of(Map.of("key", "email", "label", "Email", "type", "text", "required", true), Map.of("key", "attributes", "label", "Attributes (JSON)", "type", "json"))),
                        Map.of("actionKey", "brevo:contact:upsert", "name", "Upsert Contact", "description", "Upsert a contact", "configSchema", List.of(Map.of("key", "email", "label", "Email", "type", "text", "required", true), Map.of("key", "attributes", "label", "Attributes (JSON)", "type", "json"))),
                        Map.of("actionKey", "brevo:contact:delete", "name", "Delete Contact", "description", "Delete a contact", "configSchema", List.of(Map.of("key", "identifier", "label", "Identifier (Email or ID)", "type", "text", "required", true))),
                        Map.of("actionKey", "brevo:contact:get", "name", "Get Contact", "description", "Get a contact", "configSchema", List.of(Map.of("key", "identifier", "label", "Identifier (Email or ID)", "type", "text", "required", true))),
                        Map.of("actionKey", "brevo:contact:getAll", "name", "Get All Contacts", "description", "Get all contacts", "configSchema", List.of(Map.of("key", "limit", "label", "Limit", "type", "number", "default", 50))),
                        Map.of("actionKey", "brevo:contact:update", "name", "Update Contact", "description", "Update a contact", "configSchema", List.of(Map.of("key", "identifier", "label", "Identifier (Email or ID)", "type", "text", "required", true), Map.of("key", "attributes", "label", "Attributes (JSON)", "type", "json"))),

                        // EMAIL
                        Map.of("actionKey", "brevo:email:send", "name", "Send Email", "description", "Send a transactional email", "configSchema", List.of(Map.of("key", "senderEmail", "label", "Sender Email", "type", "text", "required", true), Map.of("key", "senderName", "label", "Sender Name", "type", "text"), Map.of("key", "recipientEmail", "label", "Recipient Email", "type", "text", "required", true), Map.of("key", "subject", "label", "Subject", "type", "text", "required", true), Map.of("key", "htmlContent", "label", "HTML Content", "type", "text", "required", true))),
                        Map.of("actionKey", "brevo:email:sendTemplate", "name", "Send Template Email", "description", "Send a transactional email via template", "configSchema", List.of(Map.of("key", "templateId", "label", "Template ID", "type", "number", "required", true), Map.of("key", "recipientEmail", "label", "Recipient Email", "type", "text", "required", true))),

                        // SENDER
                        Map.of("actionKey", "brevo:sender:create", "name", "Create Sender", "description", "Create a sender", "configSchema", List.of(Map.of("key", "name", "label", "Name", "type", "text", "required", true), Map.of("key", "email", "label", "Email", "type", "text", "required", true))),
                        Map.of("actionKey", "brevo:sender:delete", "name", "Delete Sender", "description", "Delete a sender", "configSchema", List.of(Map.of("key", "senderId", "label", "Sender ID", "type", "text", "required", true))),
                        Map.of("actionKey", "brevo:sender:getAll", "name", "Get All Senders", "description", "Get all senders", "configSchema", List.of())
                )
        ).credentialSchema(List.of(
                Map.of("key", "apiKey", "label", "API Key", "type", "password", "required", true)
        )).category("marketing");
    }
}
