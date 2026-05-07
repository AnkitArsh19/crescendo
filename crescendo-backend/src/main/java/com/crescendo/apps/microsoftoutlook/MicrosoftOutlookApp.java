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
                "Microsoft Outlook",
                "Send emails, watch for new messages, manage drafts and calendar events via Microsoft Graph",
                "/icons/microsoft-outlook.svg",
                AuthType.OAUTH2,

                // ═══ TRIGGERS ═══
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
                    ),
                    Map.of(
                        "triggerKey", "new-event",
                        "name", "New Calendar Event",
                        "description", "Triggers when a new event is created in your calendar",
                        "configSchema", List.of(
                            Map.of("key", "calendarId", "label", "Calendar",
                                   "type", "dynamic_dropdown", "resourceType", "calendars",
                                   "required", false,
                                   "helpText", "Select calendar (default: primary)")
                        )
                    )
                ),

                // ═══ ACTIONS ═══
                List.of(
                    Map.of(
                        "actionKey", "send-email",
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
                            Map.of("key", "subject", "label", "Subject",
                                   "type", "text", "required", true,
                                   "placeholder", "Hello from Crescendo",
                                   "helpText", "Email subject line"),
                            Map.of("key", "bodyHtml", "label", "Body (HTML)",
                                   "type", "textarea", "required", true,
                                   "placeholder", "<p>Your email content here...</p>",
                                   "helpText", "Email body with HTML support"),
                            Map.of("key", "importance", "label", "Importance",
                                   "type", "dropdown", "required", false,
                                   "options", List.of(
                                       Map.of("value", "low", "label", "Low"),
                                       Map.of("value", "normal", "label", "Normal"),
                                       Map.of("value", "high", "label", "High")
                                   ),
                                   "helpText", "Email importance level")
                        )
                    ),
                    Map.of(
                        "actionKey", "create-draft",
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
                        "actionKey", "reply-email",
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
                        "actionKey", "move-email",
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
                        "actionKey", "create-event",
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
                        "actionKey", "list-emails",
                        "name", "List Emails",
                        "description", "Retrieve recent emails from a mail folder",
                        "configSchema", List.of(
                            folderFieldRequired,
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
        .helpUrl("https://portal.azure.com/");
    }
}
