package com.crescendo.apps.pushbullet;

import com.crescendo.app.App;
import com.crescendo.apps.AppDefinition;
import com.crescendo.enums.AuthType;
import org.springframework.stereotype.Component;

import java.util.List;
import java.util.Map;

@Component
public class PushbulletApp implements AppDefinition {

    @Override
    public App toApp() {
        return new App("pushbullet", "Pushbullet", """
                Pushbullet bridges the gap between your phone, tablet, and computer, allowing them to share notifications and files. The Crescendo Pushbullet app lets you send instant alerts to your devices.

                **What you can do with Pushbullet in Crescendo:**
                - Send a push notification to your phone immediately when an urgent server error occurs
                - Push a summary link of your daily schedule to your tablet every morning
                - Notify yourself instantly when a specific VIP client sends an email
                - Send a quick alert when a long-running data export finally completes

                **Actions available:**
                - Send Push — push a title and message to all connected Pushbullet devices

                **Who should use this:** Solopreneurs, IT admins, and power users who need instant, cross-device personal alerts.

                **Authentication:** API Key (available in your Pushbullet account settings).
                """,
                "https://www.google.com/s2/favicons?domain=pushbullet.com&sz=128", AuthType.APIKEY,
                List.of(),
                List.of(
                        Map.of("actionKey", "send-note", "name", "Send Note",
                                "description", "Send a Pushbullet note",
                                "configSchema", List.of(
                                        Map.of("key", "title", "label", "Title", "type", "text", "required", false),
                                        Map.of("key", "body", "label", "Body", "type", "textarea", "required", true)))
                )
        ).credentialSchema(List.of(
                Map.of("key", "accessToken", "label", "Access Token", "type", "password", "required", true)
        )).category("communication").helpUrl("https://docs.pushbullet.com/");
    }
}
