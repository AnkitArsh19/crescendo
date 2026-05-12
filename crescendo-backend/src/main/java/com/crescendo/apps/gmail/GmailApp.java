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

        var messageIdField = Map.of("key", "messageId", "label", "Message ID",
                "type", "text", "required", true,
                "helpText", "The ID of the email message (from a trigger or search)");

        var labelIdField = Map.<String, Object>of("key", "labelId", "label", "Label",
                "type", "dynamic_dropdown", "resourceType", "labels",
                "required", true,
                "helpText", "Select the label to apply");

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
                    ),
                    Map.of(
                        "triggerKey", "new-email-matching-search",
                        "name", "New Email Matching Search",
                        "description", "Triggers only for emails matching a Gmail search query",
                        "configSchema", List.of(
                            Map.of("key", "searchQuery", "label", "Search Query",
                                   "type", "text", "required", true,
                                   "placeholder", "from:boss@acme.com has:attachment",
                                   "helpText", "Gmail advanced search query (same syntax as Gmail search bar)")
                        )
                    ),
                    Map.of(
                        "triggerKey", "new-email-from-person",
                        "name", "New Email from Specific Person",
                        "description", "Triggers when an email arrives from a specific sender",
                        "configSchema", List.of(
                            Map.of("key", "fromAddress", "label", "From Address",
                                   "type", "text", "required", true,
                                   "placeholder", "alice@company.com",
                                   "helpText", "Sender email address to watch"),
                            labelField
                        )
                    ),
                    Map.of(
                        "triggerKey", "new-email-with-subject",
                        "name", "New Email with Subject",
                        "description", "Triggers when an email with a specific subject arrives",
                        "configSchema", List.of(
                            Map.of("key", "subjectKeywords", "label", "Subject Keywords",
                                   "type", "text", "required", true,
                                   "placeholder", "Quarterly Report",
                                   "helpText", "Keywords to match in the subject line"),
                            labelField
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
                                   "helpText", "Email body (HTML supported)"),
                            Map.of("key", "attachment", "label", "Attachment",
                                   "type", "file", "required", false,
                                   "accept", "*/*", "maxSizeMB", 25,
                                   "helpText", "Attach a file to the email (max 25MB)")
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
                            messageIdField,
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
                        "configSchema", List.of(messageIdField, labelIdField)
                    ),
                    Map.of(
                        "actionKey", "remove-label",
                        "name", "Remove Label",
                        "description", "Remove a label from an email message",
                        "configSchema", List.of(messageIdField, labelIdField)
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
                    ),
                    Map.of(
                        "actionKey", "mark-read",
                        "name", "Mark as Read",
                        "description", "Mark an email as read",
                        "configSchema", List.of(messageIdField)
                    ),
                    Map.of(
                        "actionKey", "mark-unread",
                        "name", "Mark as Unread",
                        "description", "Mark an email as unread",
                        "configSchema", List.of(messageIdField)
                    ),
                    Map.of(
                        "actionKey", "archive-email",
                        "name", "Archive Email",
                        "description", "Archive an email (remove from inbox)",
                        "configSchema", List.of(messageIdField)
                    )
                )
        )
        .credentialSchema(List.of())
        .category("communication")
        .helpUrl("https://console.cloud.google.com/");
    }
}
