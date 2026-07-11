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
 *
 * Distinct from:
 *   - smtp        — user's own SMTP relay credentials
 *   - gmail       — user's personal Gmail account
 *   - outlook     — user's personal Outlook account
 *
 * CrescendoMail is Crescendo's own email service: verified domains, templates,
 * contacts/audiences, broadcasts, deliverability metrics, and email triggers.
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
                    Map.of("triggerKey", "email.delivered", "name", "Email Delivered", "description", "Triggers when an email is successfully delivered"),
                    Map.of("triggerKey", "email.bounced", "name", "Email Bounced", "description", "Triggers when an email bounces"),
                    Map.of("triggerKey", "email.complained", "name", "Spam Complaint", "description", "Triggers when a recipient marks an email as spam"),
                    Map.of("triggerKey", "email.opened", "name", "Email Opened", "description", "Triggers when a recipient opens an email"),
                    Map.of("triggerKey", "email.clicked", "name", "Email Clicked", "description", "Triggers when a recipient clicks a link in an email"),
                    Map.of("triggerKey", "contact.created", "name", "Contact Created", "description", "Triggers when a new contact is added to an audience"),
                    Map.of("triggerKey", "contact.unsubscribed", "name", "Contact Unsubscribed", "description", "Triggers when a contact unsubscribes from marketing emails")
                ),
                List.of(
                    Map.of("actionKey", "send",         "name", "Send Email",
                            "description", "Send a transactional or marketing email via CrescendoMail"),
                    Map.of("actionKey", "send-templated","name", "Send Templated Email",
                            "description", "Send an email using a published CrescendoMail template"),
                    Map.of("actionKey", "send-batch",   "name", "Send Batch Email",
                            "description", "Send up to 100 emails in a single workflow step"),
                    Map.of("actionKey", "create-broadcast","name", "Create Broadcast",
                            "description", "Create a broadcast targeting an audience or segment"),
                    Map.of("actionKey", "send-broadcast","name", "Send Broadcast",
                            "description", "Send or schedule an existing broadcast"),
                    Map.of("actionKey", "upsert-contact","name", "Upsert Contact",
                            "description", "Create or update a contact in an audience"),
                    Map.of("actionKey", "set-contact-property","name", "Set Contact Property",
                            "description", "Update a single property on an existing contact"),
                    Map.of("actionKey", "add-to-audience","name", "Add Contact to Audience",
                            "description", "Add a contact to an audience by email"),
                    Map.of("actionKey", "remove-from-audience","name", "Remove Contact from Audience",
                            "description", "Remove a contact from an audience"),
                    Map.of("actionKey", "suppress",     "name", "Suppress Recipient",
                            "description", "Manually suppress a recipient from all sends"),
                    Map.of("actionKey", "unsuppress",   "name", "Remove from Suppression List",
                            "description", "Lift a suppression on a recipient"),
                    Map.of("actionKey", "get-status",   "name", "Get Email Status",
                            "description", "Fetch the delivery timeline for a specific send ID"),
                    Map.of("actionKey", "wait-for-event","name", "Wait for Email Event",
                            "description", "Pause the workflow until delivered / opened / clicked / bounced, or timeout"),
                    Map.of("actionKey", "verify-domain","name", "Verify Domain",
                            "description", "Trigger an on-demand DNS recheck for a pending domain")
                )
        ).credentialSchema(List.of()).category("communication").helpUrl("");
    }
}
