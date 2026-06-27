package com.crescendo.apps.imap;

import com.crescendo.app.App;
import com.crescendo.apps.AppDefinition;
import com.crescendo.enums.AuthType;
import org.springframework.stereotype.Component;

import java.util.List;
import java.util.Map;

@Component
public class ImapEmailApp implements AppDefinition {

    @Override
    public App toApp() {
        return new App("imap-email", "Email Read (IMAP)", """
                IMAP is an Internet standard protocol used by email clients to retrieve messages from a mail server. The Crescendo IMAP app allows you to programmatically search and read your own mailbox.

                **What you can do with IMAP in Crescendo:**
                - Search for unread emails from specific high-value clients and alert your team in Microsoft Teams
                - Extract invoice PDFs attached to emails and automatically upload them to Dropbox
                - Parse incoming support emails for specific keywords and auto-tag them in Freshdesk
                - Create a daily digest of all emails received with the subject "Weekly Report"

                **Actions available:**
                - Search Emails — query a specific folder (like INBOX) using criteria (e.g., UNSEEN, FROM) to retrieve email metadata and body text

                **Who should use this:** Operations managers, administrative assistants, and developers automating email triage and processing.

                **Authentication:** IMAP Server details (Host, Port, Username, Password, SSL).
                """,
                "/icons/email.svg", AuthType.APIKEY,
                List.of(
                        Map.of("triggerKey", "email-received", "name", "Email Received",
                                "description", "Triggers when a new email is received",
                                "configSchema", List.of(
                                        Map.of("key", "mailbox", "label", "Mailbox Name", "type", "text", "required", true, "default", "INBOX"),
                                        Map.of("key", "postProcessAction", "label", "Action", "type", "options", "required", false,
                                                "options", List.of(
                                                        Map.of("name", "Mark as Read", "value", "read"),
                                                        Map.of("name", "Nothing", "value", "nothing")
                                                )),
                                        Map.of("key", "downloadAttachments", "label", "Download Attachments", "type", "boolean", "required", false),
                                        Map.of("key", "format", "label", "Format", "type", "options", "required", false,
                                                "options", List.of(
                                                        Map.of("name", "RAW", "value", "raw"),
                                                        Map.of("name", "Resolved", "value", "resolved"),
                                                        Map.of("name", "Simple", "value", "simple")
                                                )),
                                        Map.of("key", "dataPropertyAttachmentsPrefixName", "label", "Property Prefix Name", "type", "text", "required", false, "placeholder", "attachment_"),
                                        Map.of("key", "customEmailConfig", "label", "Custom Email Rules (JSON array)", "type", "text", "required", false, "placeholder", "[\"UNSEEN\"]"),
                                        Map.of("key", "forceReconnect", "label", "Force Reconnect (minutes)", "type", "number", "required", false, "placeholder", "60"),
                                        Map.of("key", "trackLastMessageId", "label", "Fetch Only New Emails", "type", "boolean", "required", false)
                                )
                        )
                ),
                List.of(
                        Map.of("actionKey", "imap:fetchEmails", "name", "Search Emails",
                                "description", "Read recent emails from an IMAP folder",
                                "configSchema", List.of(
                                        Map.of("key", "folder", "label", "Folder", "type", "text", "required", false,
                                                "placeholder", "INBOX"),
                                        Map.of("key", "subjectContains", "label", "Subject Contains", "type", "text", "required", false),
                                        Map.of("key", "fromContains", "label", "From Contains", "type", "text", "required", false),
                                        Map.of("key", "unreadOnly", "label", "Unread Only", "type", "boolean", "required", false, "helpText", "Only fetch unread/unseen emails"),
                                        Map.of("key", "markAsRead", "label", "Mark as Read", "type", "boolean", "required", false, "helpText", "Mark emails as read after fetching"),
                                        Map.of("key", "downloadAttachments", "label", "Download Attachments", "type", "boolean", "required", false, "helpText", "Download attachments as Base64"),
                                        Map.of("key", "limit", "label", "Limit", "type", "text", "required", false,
                                                "placeholder", "10")))
                )
        ).credentialSchema(List.of(
                Map.of("key", "host", "label", "IMAP Host", "type", "text", "required", true,
                        "placeholder", "imap.gmail.com"),
                Map.of("key", "port", "label", "Port", "type", "text", "required", false,
                        "placeholder", "993"),
                Map.of("key", "username", "label", "Username", "type", "text", "required", true),
                Map.of("key", "password", "label", "Password / App Password", "type", "password", "required", true),
                Map.of("key", "ssl", "label", "Use SSL", "type", "boolean", "required", false,
                        "helpText", "Use SSL/TLS, usually true for port 993")
        )).category("communication").helpUrl("");
    }
}
