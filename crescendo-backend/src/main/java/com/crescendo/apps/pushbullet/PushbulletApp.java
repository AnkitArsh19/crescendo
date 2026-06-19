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
        return new App("pushbullet", "Pushbullet", "Send pushes to Pushbullet devices",
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
