package com.crescendo.apps.brevo;

import com.crescendo.app.App;
import com.crescendo.apps.AppDefinition;
import com.crescendo.enums.AuthType;
import org.springframework.stereotype.Component;

import java.util.List;
import java.util.Map;

@Component
public class BrevoApp implements AppDefinition {

    @Override
    public App toApp() {
        return new App("brevo", "Brevo", "Send Brevo emails and manage contacts",
                "https://cdn.brandfetch.io/id87KI8yDJ/w/200/h/200/theme/dark/icon.jpeg?c=1bxid64Mup7aczewSAYMX&t=1759046719977", AuthType.OAUTH2,
                List.of(),
                List.of(
                        Map.of("actionKey", "send-email", "name", "Send Transactional Email",
                                "description", "Send an email through Brevo API",
                                "configSchema", List.of(
                                        Map.of("key", "to", "label", "To", "type", "text", "required", true),
                                        Map.of("key", "subject", "label", "Subject", "type", "text", "required", true),
                                        Map.of("key", "htmlContent", "label", "HTML Content", "type", "textarea", "required", true),
                                        Map.of("key", "senderEmail", "label", "Sender Email", "type", "text", "required", true),
                                        Map.of("key", "senderName", "label", "Sender Name", "type", "text", "required", false))),
                        Map.of("actionKey", "create-contact", "name", "Create Contact",
                                "description", "Create or update a Brevo contact",
                                "configSchema", List.of(
                                        Map.of("key", "email", "label", "Email", "type", "text", "required", true),
                                        Map.of("key", "attributes", "label", "Attributes (JSON)", "type", "json", "required", false,
                                                "placeholder", "{\"FIRSTNAME\":\"Jane\"}"),
                                        Map.of("key", "listIds", "label", "List IDs (JSON Array)", "type", "json", "required", false,
                                                "placeholder", "[2]")))
                )
        ).credentialSchema(List.of()).category("marketing").helpUrl("https://avatars.githubusercontent.com/u/100062402?v=4");
    }
}
