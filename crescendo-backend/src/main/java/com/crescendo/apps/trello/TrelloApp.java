package com.crescendo.apps.trello;

import com.crescendo.app.App;
import com.crescendo.apps.AppDefinition;
import com.crescendo.enums.AuthType;
import org.springframework.stereotype.Component;
import java.util.*;

@Component
public class TrelloApp implements AppDefinition {
    public App toApp() {
        return new App(
                "trello",
                "Trello",
                "Create cards and list board cards",
                "https://www.google.com/s2/favicons?domain=trello.com&sz=128",
                AuthType.OAUTH2,
                List.of(),
                List.of(
                        Map.of(
                                "actionKey", "create-card",
                                "name", "Create Card",
                                "description", "Create a card in a list",
                                "configSchema", List.of(
                                        Map.of("key", "listId", "label", "List ID", "type", "text", "required", true),
                                        Map.of("key", "name", "label", "Name", "type", "text", "required", true),
                                        Map.of("key", "desc", "label", "Description", "type", "textarea", "required", false)
                                )
                        ),
                        Map.of(
                                "actionKey", "list-cards",
                                "name", "List Cards",
                                "description", "List cards on a board",
                                "configSchema", List.of(
                                        Map.of("key", "boardId", "label", "Board ID", "type", "text", "required", true)
                                )
                        )
                )
        ).credentialSchema(List.of(
                Map.of("key", "apiKey", "label", "API Key", "type", "password", "required", true),
                Map.of("key", "token", "label", "Token", "type", "password", "required", true)
        )).category("productivity").helpUrl("https://developer.atlassian.com/cloud/trello/rest/");
    }
}
