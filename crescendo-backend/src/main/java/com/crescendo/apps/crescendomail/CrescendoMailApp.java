package com.crescendo.apps.crescendomail;

import com.crescendo.app.App;
import com.crescendo.apps.AppDefinition;
import com.crescendo.enums.AuthType;
import org.springframework.stereotype.Component;

import java.util.List;
import java.util.Map;

/**
 * CrescendoMail — Crescendo's built-in transactional and marketing email platform.
 *
 * App key: crescendomail
 */
@Component
public class CrescendoMailApp implements AppDefinition {

    @Override
    public App toApp() {
        return new App(
                "crescendomail",
                "CrescendoMail",
                """
                CrescendoMail is Crescendo's built-in email platform for sending transactional and marketing emails.
                Unlike Gmail or SMTP integrations (which use your personal mailbox), CrescendoMail uses Crescendo's
                own verified sending infrastructure — giving you deliverability metrics, audience management,
                template variables, and email event triggers in one place.

                **Actions available:**
                - Send Email — raw HTML/text to any recipient
                - Send Templated Email — by template ID with variable substitution
                - Send Batch Email — up to 100 recipients in one step
                - Create / Send Broadcast — target an audience or segment
                - Upsert Contact — create or update a contact in an audience
                - Set Contact Property — update a property on an existing contact
                - Add / Remove Contact from Audience
                - Suppress Recipient / Remove from Suppression List
                - Get Email Status — fetch the delivery timeline for a send
                - Wait for Email Event — pause the workflow until delivered/opened/clicked/bounced
                - Verify Domain — trigger an on-demand DNS recheck

                **Triggers:**
                - Email delivered / opened / clicked / bounced / complained
                - Recipient unsubscribed / suppression added or removed
                - Domain verification status changed
                - Contact created or updated / Contact property changed
                - Custom event received

                **Authentication:** None required — uses your configured verified sending domain.
                """,
                "/icons/crescendomail.svg",
                AuthType.NONE,
                List.of(
                    Map.of(
                        "triggerKey", "email.delivered",
                        "name", "Email Delivered",
                        "description", "Triggers when an email sent via CrescendoMail is successfully delivered",
                        "configSchema", List.of()
                    ),
                    Map.of(
                        "triggerKey", "email.opened",
                        "name", "Email Opened",
                        "description", "Triggers when a recipient opens an email sent via CrescendoMail",
                        "configSchema", List.of()
                    ),
                    Map.of(
                        "triggerKey", "email.clicked",
                        "name", "Email Clicked",
                        "description", "Triggers when a recipient clicks a tracked link in an email",
                        "configSchema", List.of()
                    ),
                    Map.of(
                        "triggerKey", "email.bounced",
                        "name", "Email Bounced",
                        "description", "Triggers when an email bounces (hard or soft bounce)",
                        "configSchema", List.of()
                    ),
                    Map.of(
                        "triggerKey", "email.complained",
                        "name", "Spam Complaint",
                        "description", "Triggers when a recipient flags an email as spam",
                        "configSchema", List.of()
                    ),
                    Map.of(
                        "triggerKey", "contact.created",
                        "name", "Contact Created",
                        "description", "Triggers when a new contact is added to an audience",
                        "configSchema", List.of()
                    ),
                    Map.of(
                        "triggerKey", "contact.unsubscribed",
                        "name", "Contact Unsubscribed",
                        "description", "Triggers when a contact unsubscribes from marketing communications",
                        "configSchema", List.of()
                    )
                ),
                List.of(
                    // ── 1. Send Email (Raw) ──
                    Map.of(
                        "actionKey", "send",
                        "name", "Send Email",
                        "description", "Send a transactional or marketing email via CrescendoMail with custom HTML/text",
                        "configSchema", List.of(
                            Map.of("key", "to", "label", "To (Recipient)", "type", "text", "required", true, "placeholder", "user@example.com or {{steps.1.email}}"),
                            Map.of("key", "from", "label", "From (Sender)", "type", "text", "required", true, "placeholder", "notifications@yourdomain.com"),
                            Map.of("key", "subject", "label", "Subject", "type", "text", "required", true, "placeholder", "Welcome to Crescendo!"),
                            Map.of("key", "htmlBody", "label", "HTML Body", "type", "textarea", "required", true, "placeholder", "<p>Hello {{steps.1.name}}, welcome to our platform!</p>"),
                            Map.of("key", "textBody", "label", "Plain Text Body", "type", "textarea", "required", false, "placeholder", "Hello {{steps.1.name}}, welcome to our platform!"),
                            Map.of("key", "emailType", "label", "Email Type", "type", "select", "required", false, "options", List.of("TRANSACTIONAL", "MARKETING"), "default", "TRANSACTIONAL")
                        )
                    ),
                    // ── 2. Send Templated Email ──
                    Map.of(
                        "actionKey", "send-templated",
                        "name", "Send Templated Email",
                        "description", "Send an email using a published CrescendoMail template with variable substitution",
                        "configSchema", List.of(
                            Map.of("key", "to", "label", "To (Recipient)", "type", "text", "required", true, "placeholder", "user@example.com or {{steps.1.email}}"),
                            Map.of("key", "from", "label", "From (Sender)", "type", "text", "required", true, "placeholder", "hello@yourdomain.com"),
                            Map.of("key", "templateId", "label", "Template ID", "type", "text", "required", true, "placeholder", "UUID of published template"),
                            Map.of("key", "variables", "label", "Variables (JSON Map)", "type", "json", "required", false, "placeholder", "{\"FIRST_NAME\": \"{{steps.1.name}}\", \"ORDER_ID\": \"1042\"}"),
                            Map.of("key", "emailType", "label", "Email Type", "type", "select", "required", false, "options", List.of("TRANSACTIONAL", "MARKETING"), "default", "TRANSACTIONAL")
                        )
                    ),
                    // ── 3. Send Batch Email ──
                    Map.of(
                        "actionKey", "send-batch",
                        "name", "Send Batch Email",
                        "description", "Send up to 100 emails in a single step with shared or per-recipient variables",
                        "configSchema", List.of(
                            Map.of("key", "from", "label", "From (Sender)", "type", "text", "required", true, "placeholder", "team@yourdomain.com"),
                            Map.of("key", "subject", "label", "Subject", "type", "text", "required", true, "placeholder", "Important update for {{name}}"),
                            Map.of("key", "htmlBody", "label", "HTML Body", "type", "textarea", "required", false, "placeholder", "<p>Hi {{name}}, here is your update...</p>"),
                            Map.of("key", "templateId", "label", "Template ID (Optional)", "type", "text", "required", false, "placeholder", "UUID of template (alternative to HTML body)"),
                            Map.of("key", "recipients", "label", "Recipients (JSON Array)", "type", "json", "required", true, "placeholder", "[{\"to\": \"user1@example.com\", \"name\": \"Alice\"}, {\"to\": \"user2@example.com\", \"name\": \"Bob\"}]"),
                            Map.of("key", "emailType", "label", "Email Type", "type", "select", "required", false, "options", List.of("TRANSACTIONAL", "MARKETING"), "default", "TRANSACTIONAL")
                        )
                    ),
                    // ── 4. Create Broadcast ──
                    Map.of(
                        "actionKey", "create-broadcast",
                        "name", "Create Broadcast",
                        "description", "Create a draft broadcast targeting your audience with a template",
                        "configSchema", List.of(
                            Map.of("key", "templateId", "label", "Template ID", "type", "text", "required", true, "placeholder", "UUID of published template"),
                            Map.of("key", "fromAddress", "label", "From Address", "type", "text", "required", true, "placeholder", "newsletter@yourdomain.com")
                        )
                    ),
                    // ── 5. Send Broadcast ──
                    Map.of(
                        "actionKey", "send-broadcast",
                        "name", "Send Broadcast",
                        "description", "Send or schedule an existing draft broadcast to all subscribed contacts",
                        "configSchema", List.of(
                            Map.of("key", "broadcastId", "label", "Broadcast ID", "type", "text", "required", true, "placeholder", "UUID of broadcast")
                        )
                    ),
                    // ── 6. Upsert Contact ──
                    Map.of(
                        "actionKey", "upsert-contact",
                        "name", "Upsert Contact",
                        "description", "Create or update a contact in your audience by email",
                        "configSchema", List.of(
                            Map.of("key", "email", "label", "Email Address", "type", "text", "required", true, "placeholder", "lead@company.com"),
                            Map.of("key", "firstName", "label", "First Name", "type", "text", "required", false, "placeholder", "Jane"),
                            Map.of("key", "lastName", "label", "Last Name", "type", "text", "required", false, "placeholder", "Doe")
                        )
                    ),
                    // ── 7. Set Contact Property ──
                    Map.of(
                        "actionKey", "set-contact-property",
                        "name", "Set Contact Property",
                        "description", "Update a single property (firstName, lastName, subscribed) on an existing contact",
                        "configSchema", List.of(
                            Map.of("key", "email", "label", "Contact Email", "type", "text", "required", true, "placeholder", "user@example.com"),
                            Map.of("key", "property", "label", "Property Name", "type", "select", "required", true, "options", List.of("firstName", "lastName", "subscribed"), "default", "subscribed"),
                            Map.of("key", "value", "label", "Property Value", "type", "text", "required", true, "placeholder", "true or updated value")
                        )
                    ),
                    // ── 8. Add Contact to Audience ──
                    Map.of(
                        "actionKey", "add-to-audience",
                        "name", "Add Contact to Audience",
                        "description", "Add a contact and mark them as subscribed in your audience",
                        "configSchema", List.of(
                            Map.of("key", "email", "label", "Contact Email", "type", "text", "required", true, "placeholder", "user@example.com")
                        )
                    ),
                    // ── 9. Remove Contact from Audience ──
                    Map.of(
                        "actionKey", "remove-from-audience",
                        "name", "Remove Contact from Audience",
                        "description", "Unsubscribe a contact from marketing broadcasts",
                        "configSchema", List.of(
                            Map.of("key", "email", "label", "Contact Email", "type", "text", "required", true, "placeholder", "user@example.com")
                        )
                    ),
                    // ── 10. Suppress Recipient ──
                    Map.of(
                        "actionKey", "suppress",
                        "name", "Suppress Recipient",
                        "description", "Manually add a recipient to the suppression list to block future sends",
                        "configSchema", List.of(
                            Map.of("key", "email", "label", "Email Address", "type", "text", "required", true, "placeholder", "user@example.com"),
                            Map.of("key", "reason", "label", "Reason", "type", "text", "required", false, "placeholder", "manual request or bounce")
                        )
                    ),
                    // ── 11. Remove from Suppression List ──
                    Map.of(
                        "actionKey", "unsuppress",
                        "name", "Remove from Suppression List",
                        "description", "Remove a recipient from suppression list to resume sending",
                        "configSchema", List.of(
                            Map.of("key", "suppressionId", "label", "Suppression ID", "type", "text", "required", true, "placeholder", "UUID of suppression record")
                        )
                    ),
                    // ── 12. Get Email Status ──
                    Map.of(
                        "actionKey", "get-status",
                        "name", "Get Email Status",
                        "description", "Fetch the delivery and engagement status (open count, click count) for a send",
                        "configSchema", List.of(
                            Map.of("key", "emailId", "label", "Email ID", "type", "text", "required", true, "placeholder", "UUID from previous send step")
                        )
                    ),
                    // ── 13. Wait for Email Event ──
                    Map.of(
                        "actionKey", "wait-for-event",
                        "name", "Wait for Email Event",
                        "description", "Pause workflow execution until an email event (delivered, opened, clicked, bounced) occurs",
                        "configSchema", List.of(
                            Map.of("key", "emailId", "label", "Email ID", "type", "text", "required", true, "placeholder", "UUID of send to watch"),
                            Map.of("key", "event", "label", "Event to Wait For", "type", "select", "required", true, "options", List.of("delivered", "opened", "clicked", "bounced"), "default", "delivered"),
                            Map.of("key", "timeoutHours", "label", "Timeout (Hours)", "type", "number", "required", false, "placeholder", "24", "default", 24)
                        )
                    ),
                    // ── 14. Verify Domain ──
                    Map.of(
                        "actionKey", "verify-domain",
                        "name", "Verify Domain",
                        "description", "Trigger an on-demand DNS verification check for a sending domain",
                        "configSchema", List.of(
                            Map.of("key", "domainId", "label", "Domain ID", "type", "text", "required", true, "placeholder", "UUID of domain to verify")
                        )
                    )
                )
        ).credentialSchema(List.of()).category("communication").helpUrl("");
    }
}
