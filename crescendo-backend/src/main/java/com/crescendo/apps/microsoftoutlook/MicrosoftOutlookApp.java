package com.crescendo.apps.microsoftoutlook;

import com.crescendo.app.App;
import com.crescendo.apps.AppDefinition;
import com.crescendo.enums.AuthType;
import org.springframework.stereotype.Component;

import java.util.List;
import java.util.Map;

@Component
public class MicrosoftOutlookApp implements AppDefinition {

    @Override
    public App toApp() {
        var folderField = Map.of("key", "folderId", "label", "Mail Folder",
                "type", "dynamic_dropdown", "resourceType", "mailFolders",
                "required", false,
                "helpText", "Select a folder to watch (default: Inbox)");

        var folderFieldRequired = Map.of("key", "folderId", "label", "Mail Folder",
                "type", "dynamic_dropdown", "resourceType", "mailFolders",
                "required", true,
                "helpText", "Select which mail folder");

        return new App(
                "microsoft-outlook",
                "Microsoft Outlook", """
                Microsoft Outlook is a personal information manager used primarily as an email application. The Crescendo Outlook app lets you send emails, manage drafts, and automate your inbox using Microsoft Graph.

                **What you can do with Outlook in Crescendo:**
                - Automatically reply to specific emails based on subject filters
                - Draft an email summary using Gemini and save it to your Drafts folder
                - Move incoming invoices directly to an "Accounting" folder
                - Forward important alerts to Microsoft Teams

                **Triggers available:**
                - New Email — trigger a workflow when a message arrives in a specific folder

                **Actions available:**
                - Send Email — compose and send an HTML email
                - Create Draft — save a message without sending
                - Reply to Email / Move Email — manage existing messages
                - List Emails — fetch recent inbox activity

                **Who should use this:** Professionals managing high-volume inboxes, executive assistants, and enterprise teams using Office 365.

                **Authentication:** OAuth 2.0 (connect your Microsoft account).
                """,
                "https://upload.wikimedia.org/wikipedia/commons/c/cc/Microsoft_Outlook_Icon_%282025%E2%80%93present%29.svg",
                AuthType.OAUTH2,

                // ═══ TRIGGERS ═══
                // Only triggers with a real PollingTriggerScheduler implementation are listed.
                // new-event (calendar) is omitted until its poller is implemented.
                List.of(
                    Map.of(
                        "triggerKey", "new-email",
                        "name", "New Email",
                        "description", "Triggers when a new email arrives in your inbox or a specific folder",
                        "configSchema", List.of(
                            folderField,
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
                        "actionKey", "sendEmail",
                        "name", "Send Email",
                        "description", "Send an email from your Outlook account",
                        "configSchema", List.of(
                            Map.of("key", "to", "label", "To",
                                   "type", "text", "required", true,
                                   "placeholder", "recipient@example.com",
                                   "helpText", "Recipient email address (comma-separated for multiple)"),
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
                            Map.of("key", "bodyContentType", "label", "Body Content Type",
                                   "type", "dropdown", "required", false,
                                   "options", List.of(Map.of("value", "html", "label", "HTML"), Map.of("value", "text", "label", "Text")),
                                   "helpText", "Message body format (default: HTML)"),
                            Map.of("key", "bodyHtml", "label", "Body Content",
                                   "type", "textarea", "required", true,
                                   "placeholder", "Your email content here...",
                                   "helpText", "Email body content"),
                            Map.of("key", "importance", "label", "Importance",
                                   "type", "dropdown", "required", false,
                                   "options", List.of(
                                       Map.of("value", "low", "label", "Low"),
                                       Map.of("value", "normal", "label", "Normal"),
                                       Map.of("value", "high", "label", "High")
                                   ),
                                   "helpText", "Email importance level"),
                            Map.of("key", "isReadReceiptRequested", "label", "Read Receipt Requested",
                                   "type", "dropdown", "required", false,
                                   "options", List.of(Map.of("value", "true", "label", "True"), Map.of("value", "false", "label", "False"))),
                            Map.of("key", "replyTo", "label", "Reply To",
                                   "type", "text", "required", false,
                                   "helpText", "Email address to use when replying"),
                            Map.of("key", "saveToSentItems", "label", "Save to Sent Items",
                                   "type", "dropdown", "required", false,
                                   "options", List.of(Map.of("value", "true", "label", "True"), Map.of("value", "false", "label", "False"))),
                            Map.of("key", "customHeaders", "label", "Custom Headers (JSON)",
                                   "type", "textarea", "required", false,
                                   "placeholder", "{\"X-Custom-Header\": \"Value\"}",
                                   "helpText", "Custom internet message headers as a JSON object")
                        )
                    ),
                    Map.of(
                        "actionKey", "createDraft",
                        "name", "Create Draft",
                        "description", "Create a draft email in your Outlook drafts folder",
                        "configSchema", List.of(
                            Map.of("key", "to", "label", "To",
                                   "type", "text", "required", true,
                                   "placeholder", "recipient@example.com",
                                   "helpText", "Recipient email address"),
                            Map.of("key", "subject", "label", "Subject",
                                   "type", "text", "required", true,
                                   "placeholder", "Draft: Meeting Notes",
                                   "helpText", "Subject for the draft email"),
                            Map.of("key", "bodyHtml", "label", "Body (HTML)",
                                   "type", "textarea", "required", true,
                                   "helpText", "Email body content")
                        )
                    ),
                    Map.of(
                        "actionKey", "replyMessage",
                        "name", "Reply to Email",
                        "description", "Reply to an existing email message",
                        "configSchema", List.of(
                            Map.of("key", "messageId", "label", "Message ID",
                                   "type", "text", "required", true,
                                   "helpText", "The ID of the email message to reply to (from a trigger)"),
                            Map.of("key", "comment", "label", "Reply Body",
                                   "type", "textarea", "required", true,
                                   "placeholder", "Thanks for the update!",
                                   "helpText", "Your reply message content")
                        )
                    ),
                    Map.of(
                        "actionKey", "moveMessage",
                        "name", "Move Email to Folder",
                        "description", "Move an email message to a different folder",
                        "configSchema", List.of(
                            Map.of("key", "messageId", "label", "Message ID",
                                   "type", "text", "required", true,
                                   "helpText", "The ID of the email to move"),
                            folderFieldRequired
                        )
                    ),
                    Map.of(
                        "actionKey", "createEvent",
                        "name", "Create Calendar Event",
                        "description", "Create a new event on your Outlook calendar",
                        "configSchema", List.of(
                            Map.of("key", "subject", "label", "Event Title",
                                   "type", "text", "required", true,
                                   "placeholder", "Team Standup",
                                   "helpText", "Title of the calendar event"),
                            Map.of("key", "start", "label", "Start Date/Time",
                                   "type", "text", "required", true,
                                   "placeholder", "2025-03-15T10:00:00",
                                   "helpText", "Start datetime in ISO 8601 format"),
                            Map.of("key", "end", "label", "End Date/Time",
                                   "type", "text", "required", true,
                                   "placeholder", "2025-03-15T11:00:00",
                                   "helpText", "End datetime in ISO 8601 format"),
                            Map.of("key", "body", "label", "Description",
                                   "type", "textarea", "required", false,
                                   "helpText", "Optional event body/description"),
                            Map.of("key", "attendees", "label", "Attendees",
                                   "type", "text", "required", false,
                                   "placeholder", "alice@company.com, bob@company.com",
                                   "helpText", "Comma-separated attendee emails"),
                            Map.of("key", "timeZone", "label", "Timezone",
                                   "type", "text", "required", false,
                                   "placeholder", "Asia/Kolkata",
                                   "helpText", "IANA timezone (default: UTC)")
                        )
                    ),
                    Map.of(
                        "actionKey", "getAllMessages",
                        "name", "List Emails",
                        "description", "Retrieve recent emails from a mail folder",
                        "configSchema", List.of(
                            folderFieldRequired,
                            Map.of("key", "maxResults", "label", "Max Results",
                                   "type", "text", "required", false,
                                   "placeholder", "10",
                                   "helpText", "Maximum emails to return"),
                            Map.of("key", "search", "label", "Search",
                                   "type", "text", "required", false,
                                   "helpText", "Search term (from, subject, body)"),
                            Map.of("key", "customFilter", "label", "Custom Filter Query",
                                   "type", "text", "required", false,
                                   "placeholder", "isRead eq false",
                                   "helpText", "OData $filter query"),
                            Map.of("key", "hasAttachments", "label", "Has Attachments",
                                   "type", "dropdown", "required", false,
                                   "options", List.of(Map.of("value", "true", "label", "True"), Map.of("value", "false", "label", "False"))),
                            Map.of("key", "readStatus", "label", "Read Status",
                                   "type", "dropdown", "required", false,
                                   "options", List.of(Map.of("value", "both", "label", "Both"), Map.of("value", "unread", "label", "Unread"), Map.of("value", "read", "label", "Read"))),
                            Map.of("key", "receivedAfter", "label", "Received After",
                                   "type", "text", "required", false,
                                   "placeholder", "2025-01-01T00:00:00Z"),
                            Map.of("key", "receivedBefore", "label", "Received Before",
                                   "type", "text", "required", false,
                                   "placeholder", "2025-12-31T23:59:59Z"),
                            Map.of("key", "sender", "label", "Sender",
                                   "type", "text", "required", false,
                                   "helpText", "Sender email address")
                        )
                    )
                )
        )
        .credentialSchema(List.of())
        .category("communication")
        .helpUrl("https://portal.azure.com/");
    }
}
