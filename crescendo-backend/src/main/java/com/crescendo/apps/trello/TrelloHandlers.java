package com.crescendo.apps.trello;

import com.crescendo.apps.simpleapi.SimpleApiSupport;
import com.crescendo.execution.action.*;
import tools.jackson.databind.ObjectMapper;
import org.springframework.web.client.RestClient;

class TrelloBase {
    static String auth(ActionContext c) {
        return "key=" + SimpleApiSupport.cred(c, "apiKey") + "&token=" + SimpleApiSupport.cred(c, "token");
    }
}

@ActionMapping(appKey = "trello", actionKey = "create-card")
class TrelloCreateCardHandler implements ActionHandler {
    private final ObjectMapper m;

    TrelloCreateCardHandler(ObjectMapper m) {
        this.m = m;
    }

    @Override
    public ActionResult execute(ActionContext c) {
        try {
            String res = RestClient.create("https://api.trello.com/1").post()
                    .uri("/cards?" + TrelloBase.auth(c) + "&idList={list}&name={name}&desc={desc}",
                            SimpleApiSupport.cfg(c, "listId"),
                            SimpleApiSupport.cfg(c, "name"),
                            SimpleApiSupport.cfg(c, "desc"))
                    .retrieve()
                    .body(String.class);
            return SimpleApiSupport.parsed(m, res);
        } catch (Exception e) {
            return ActionResult.failure("Trello create card failed: " + e.getMessage());
        }
    }
}

@ActionMapping(appKey = "trello", actionKey = "list-cards")
class TrelloListCardsHandler implements ActionHandler {
    private final ObjectMapper m;

    TrelloListCardsHandler(ObjectMapper m) {
        this.m = m;
    }

    @Override
    public ActionResult execute(ActionContext c) {
        try {
            String res = RestClient.create("https://api.trello.com/1").get()
                    .uri("/boards/{board}/cards?" + TrelloBase.auth(c), SimpleApiSupport.cfg(c, "boardId"))
                    .retrieve()
                    .body(String.class);
            return SimpleApiSupport.parsed(m, res);
        } catch (Exception e) {
            return ActionResult.failure("Trello list cards failed: " + e.getMessage());
        }
    }
}
