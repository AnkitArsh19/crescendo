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
        return new App("imap-email", "Email Read (IMAP)", "Read emails from a user-owned IMAP mailbox",
                "/icons/email.svg", AuthType.APIKEY,
                List.of(),
                List.of(
                        Map.of("actionKey", "search-emails", "name", "Search Emails",
                                "description", "Read recent emails from an IMAP folder",
                                "configSchema", List.of(
                                        Map.of("key", "folder", "label", "Folder", "type", "text", "required", false,
                                                "placeholder", "INBOX"),
                                        Map.of("key", "subjectContains", "label", "Subject Contains", "type", "text", "required", false),
                                        Map.of("key", "fromContains", "label", "From Contains", "type", "text", "required", false),
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
