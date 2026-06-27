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

        return new App("gmail", "Gmail", """
                Gmail is Google's widely used email service. The Crescendo Gmail app lets you automate your inbox, send emails on behalf of users, and build workflows that react to new incoming messages.

                **What you can do with Gmail in Crescendo:**
                - Send emails, including rich HTML bodies, CC, and BCC
                - Trigger workflows when new emails arrive matching specific criteria
                - Organize your inbox by adding or removing labels automatically
                - Search your inbox using standard Gmail search syntax
                - Create drafts for later review
                - Manage email state (mark as read, unread, or archive)

                **Triggers available:**
                - New Email Received — fire a workflow when an email hits your inbox, with optional filters for subject, sender, or label

                **Who should use this:** Customer support teams for auto-replying to common queries, sales teams for sending automated follow-ups, and anyone looking to automate repetitive inbox management tasks.

                **Authentication:** OAuth 2.0 (connect your Google account).
                """,
                "https://ssl.gstatic.com/images/branding/product/2x/gmail_2020q4_48dp.png", AuthType.OAUTH2,

                // ═══ TRIGGERS ═══
                // Only triggers with a real PollingTriggerScheduler implementation are listed here.
                // Unsupported trigger keys (new-attachment, new-labeled-email, etc.) are omitted
                // until their pollers are implemented, to prevent silent no-op activations.
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
                    )
                ),

                // ═══ ACTIONS ═══
                List.of(
                    Map.of(
                        "actionKey", "send",
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
                            Map.of("key", "threadId", "label", "Thread ID",
                                   "type", "text", "required", false,
                                   "helpText", "The ID of the thread to reply to (optional)")
                        )
                    ),
                    Map.of(
                        "actionKey", "createDraft",
                        "name", "Create Draft",
                        "description", "Create a draft email in Gmail",
                        "configSchema", List.of(
                            Map.of("key", "to", "label", "To",
                                   "type", "text", "required", true,
                                   "placeholder", "recipient@example.com",
                                   "helpText", "Recipient email address"),
                            Map.of("key", "cc", "label", "CC",
                                   "type", "text", "required", false,
                                   "helpText", "CC recipients (optional)"),
                            Map.of("key", "bcc", "label", "BCC",
                                   "type", "text", "required", false,
                                   "helpText", "BCC recipients (optional)"),
                            Map.of("key", "subject", "label", "Subject",
                                   "type", "text", "required", true,
                                   "helpText", "Subject for the draft email"),
                            Map.of("key", "body", "label", "Body",
                                   "type", "textarea", "required", true,
                                   "helpText", "Draft email body"),
                            Map.of("key", "threadId", "label", "Thread ID",
                                   "type", "text", "required", false,
                                   "helpText", "The ID of the thread to attach the draft (optional)")
                        )
                    ),
                    Map.of(
                        "actionKey", "reply",
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
                        "actionKey", "addLabels",
                        "name", "Add Label",
                        "description", "Add a label to an email message",
                        "configSchema", List.of(messageIdField, labelIdField)
                    ),
                    Map.of(
                        "actionKey", "removeLabels",
                        "name", "Remove Label",
                        "description", "Remove a label from an email message",
                        "configSchema", List.of(messageIdField, labelIdField)
                    ),
                    Map.of(
                        "actionKey", "search",
                        "name", "Search Emails",
                        "description", "Search for emails matching a query",
                        "configSchema", List.of(
                            Map.of("key", "query", "label", "Search Query",
                                   "type", "text", "required", false,
                                   "placeholder", "from:boss@company.com subject:invoice",
                                   "helpText", "Gmail search query (same syntax as Gmail search bar)"),
                            Map.of("key", "labelIds", "label", "Labels",
                                   "type", "dynamic_dropdown", "resourceType", "labels",
                                   "required", false,
                                   "helpText", "Only return messages with these labels"),
                            Map.of("key", "includeSpamTrash", "label", "Include Spam and Trash",
                                   "type", "select", "required", false,
                                   "options", List.of(
                                       Map.of("label", "No", "value", "false"),
                                       Map.of("label", "Yes", "value", "true")
                                   ),
                                   "helpText", "Whether to include messages from SPAM and TRASH"),
                            Map.of("key", "unread", "label", "Unread Only",
                                   "type", "select", "required", false,
                                   "options", List.of(
                                       Map.of("label", "No", "value", "false"),
                                       Map.of("label", "Yes", "value", "true")
                                   ),
                                   "helpText", "Only return unread messages"),
                            Map.of("key", "maxResults", "label", "Max Results",
                                   "type", "text", "required", false,
                                   "placeholder", "10",
                                   "helpText", "Maximum emails to return")
                        )
                    ),
                    Map.of(
                        "actionKey", "markAsRead",
                        "name", "Mark as Read",
                        "description", "Mark an email as read",
                        "configSchema", List.of(messageIdField)
                    ),
                    Map.of(
                        "actionKey", "markAsUnread",
                        "name", "Mark as Unread",
                        "description", "Mark an email as unread",
                        "configSchema", List.of(messageIdField)
                    ),
                    Map.of(
                        "actionKey", "delete",
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
