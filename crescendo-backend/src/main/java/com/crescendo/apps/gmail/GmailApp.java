package com.crescendo.apps.gmail;

import com.crescendo.app.App;
import com.crescendo.apps.AppDefinition;
import com.crescendo.enums.AuthType;
import org.springframework.stereotype.Component;

import java.util.List;
import java.util.Map;

@Component
public class GmailApp implements AppDefinition {

    @Override
    public App toApp() {
        var labelField = Map.of("key", "labelFilter", "label", "Label",
                "type", "dynamic_dropdown", "resourceType", "labels",
                "required", false,
                "helpText", "Optionally filter by Gmail label (e.g. INBOX, IMPORTANT)");

        return new App("gmail", "Gmail", "Send, search, and watch emails via the Gmail API",
                "/icons/gmail.svg", AuthType.OAUTH2,

                // ═══ TRIGGERS ═══
                List.of(
                    Map.of(
                        "triggerKey", "new-email",
                        "name", "New Email Received",
                        "description", "Triggers when a new email arrives",
                        "configSchema", List.of(
                            labelField,
                            Map.of("key", "subjectFilter", "label", "Subject Contains",
                                   "type", "text", "required", false,
                                   "placeholder", "Invoice",
                                   "helpText", "Optionally filter by subject keyword"),
                            Map.of("key", "fromFilter", "label", "From Address Contains",
                                   "type", "text", "required", false,
                                   "placeholder", "boss@company.com",
                                   "helpText", "Optionally filter by sender address")
                        )
                    ),
                    Map.of(
                        "triggerKey", "new-attachment",
                        "name", "New Attachment",
                        "description", "Triggers when a new email with an attachment arrives",
                        "configSchema", List.of(labelField)
                    ),
                    Map.of(
                        "triggerKey", "new-labeled-email",
                        "name", "New Labeled Email",
                        "description", "Triggers when an email receives a specific label",
                        "configSchema", List.of(
                            Map.of("key", "labelId", "label", "Label",
                                   "type", "dynamic_dropdown", "resourceType", "labels",
                                   "required", true,
                                   "helpText", "Select the Gmail label to watch")
                        )
                    )
                ),

                // ═══ ACTIONS ═══
                List.of(
                    Map.of(
                        "actionKey", "send-email",
                        "name", "Send Email",
                        "description", "Send an email via Gmail",
                        "configSchema", List.of(
                            Map.of("key", "to", "label", "To",
                                   "type", "text", "required", true,
                                   "placeholder", "recipient@example.com",
                                   "helpText", "Recipient email address"),
                            Map.of("key", "cc", "label", "CC",
                                   "type", "text", "required", false,
                                   "placeholder", "cc@example.com",
                                   "helpText", "CC recipients (optional)"),
                            Map.of("key", "bcc", "label", "BCC",
                                   "type", "text", "required", false,
                                   "placeholder", "bcc@example.com",
                                   "helpText", "BCC recipients (optional)"),
                            Map.of("key", "subject", "label", "Subject",
                                   "type", "text", "required", true,
                                   "placeholder", "Hello from Crescendo",
                                   "helpText", "Email subject line"),
                            Map.of("key", "body", "label", "Body",
                                   "type", "textarea", "required", true,
                                   "placeholder", "<p>Your email content here...</p>",
                                   "helpText", "Email body (HTML supported)")
                        )
                    ),
                    Map.of(
                        "actionKey", "create-draft",
                        "name", "Create Draft",
                        "description", "Create a draft email in Gmail",
                        "configSchema", List.of(
                            Map.of("key", "to", "label", "To",
                                   "type", "text", "required", true,
                                   "placeholder", "recipient@example.com",
                                   "helpText", "Recipient email address"),
                            Map.of("key", "subject", "label", "Subject",
                                   "type", "text", "required", true,
                                   "helpText", "Subject for the draft email"),
                            Map.of("key", "body", "label", "Body",
                                   "type", "textarea", "required", true,
                                   "helpText", "Draft email body")
                        )
                    ),
                    Map.of(
                        "actionKey", "reply-email",
                        "name", "Reply to Email",
                        "description", "Reply to an existing email thread",
                        "configSchema", List.of(
                            Map.of("key", "messageId", "label", "Message ID",
                                   "type", "text", "required", true,
                                   "helpText", "The ID of the email message to reply to (from a trigger)"),
                            Map.of("key", "body", "label", "Reply Body",
                                   "type", "textarea", "required", true,
                                   "placeholder", "Thanks for the update!",
                                   "helpText", "Your reply message content")
                        )
                    ),
                    Map.of(
                        "actionKey", "add-label",
                        "name", "Add Label",
                        "description", "Add a label to an email message",
                        "configSchema", List.of(
                            Map.of("key", "messageId", "label", "Message ID",
                                   "type", "text", "required", true,
                                   "helpText", "The ID of the email to label"),
                            Map.of("key", "labelId", "label", "Label",
                                   "type", "dynamic_dropdown", "resourceType", "labels",
                                   "required", true,
                                   "helpText", "Select the label to apply")
                        )
                    ),
                    Map.of(
                        "actionKey", "search-emails",
                        "name", "Search Emails",
                        "description", "Search for emails matching a query",
                        "configSchema", List.of(
                            Map.of("key", "query", "label", "Search Query",
                                   "type", "text", "required", true,
                                   "placeholder", "from:boss@company.com subject:invoice",
                                   "helpText", "Gmail search query (same syntax as Gmail search bar)"),
                            Map.of("key", "maxResults", "label", "Max Results",
                                   "type", "text", "required", false,
                                   "placeholder", "10",
                                   "helpText", "Maximum emails to return")
                        )
                    )
                )
        )
        .credentialSchema(List.of())
        .category("communication")
        .helpUrl("https://console.cloud.google.com/");
    }
}
