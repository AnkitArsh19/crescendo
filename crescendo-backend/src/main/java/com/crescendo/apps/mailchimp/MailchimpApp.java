package com.crescendo.apps.mailchimp;

import com.crescendo.app.App;
import com.crescendo.apps.AppDefinition;
import com.crescendo.enums.AuthType;
import org.springframework.stereotype.Component;
import java.util.*;

@Component
public class MailchimpApp implements AppDefinition {
    public App toApp() {
        return new App(
                "mailchimp",
                "Mailchimp",
                "Manage Mailchimp audience members",
                "https://www.google.com/s2/favicons?domain=mailchimp.com&sz=128",
                AuthType.OAUTH2,
                List.of(),
                List.of(
                        Map.of(
                                "actionKey", "add-subscriber",
                                "name", "Add Subscriber",
                                "description", "Add or update an audience member",
                                "configSchema", List.of(
                                        Map.of("key", "listId", "label", "Audience/List ID", "type", "text", "required", true),
                                        Map.of("key", "email", "label", "Email", "type", "text", "required", true),
                                        Map.of("key", "status", "label", "Status", "type", "text", "required", false, "placeholder", "subscribed")
                                )
                        )
                )
        ).credentialSchema(List.of(
                Map.of("key", "apiKey", "label", "API Key", "type", "password", "required", true),
                Map.of("key", "serverPrefix", "label", "Server Prefix", "type", "text", "required", true, "placeholder", "us21")
        )).category("marketing").helpUrl("https://mailchimp.com/developer/marketing/api/");
    }
}
