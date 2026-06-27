package com.crescendo.apps.trello;

import com.crescendo.execution.action.ActionContext;
import com.crescendo.execution.action.ActionMapping;
import com.crescendo.utils.RestClient;
import org.springframework.stereotype.Component;

import java.util.HashMap;
import java.util.Map;

/**
 * Trello Card handlers.
 */
@Component
public class TrelloCardHandlers {

    @ActionMapping(appKey = "trello", actionKey = "trello:card:create")
    public Object createCard(ActionContext context) throws Exception {
        String listId = context.getString("listId");
        String name = context.getString("name");
        String description = context.getString("description");

        Map<String, Object> body = new HashMap<>();
        body.put("idList", listId);
        body.put("name", name);
        if (description != null) body.put("desc", description);

        return RestClient.builder()
                .url(TrelloSupport.getBaseUrl() + "/cards" + TrelloSupport.getAuthQuery(context, true))
                .header("Content-Type", "application/json")
                .post(body)
                .execute();
    }

    @ActionMapping(appKey = "trello", actionKey = "trello:card:delete")
    public Object deleteCard(ActionContext context) throws Exception {
        String cardId = context.getString("cardId");
        return RestClient.builder()
                .url(TrelloSupport.getBaseUrl() + "/cards/" + cardId + TrelloSupport.getAuthQuery(context, true))
                .delete()
                .execute();
    }

    @ActionMapping(appKey = "trello", actionKey = "trello:card:get")
    public Object getCard(ActionContext context) throws Exception {
        String cardId = context.getString("cardId");
        return RestClient.builder()
                .url(TrelloSupport.getBaseUrl() + "/cards/" + cardId + TrelloSupport.getAuthQuery(context, true))
                .get()
                .execute();
    }

    @ActionMapping(appKey = "trello", actionKey = "trello:card:update")
    public Object updateCard(ActionContext context) throws Exception {
        String cardId = context.getString("cardId");
        Map<String, Object> fields = context.getMap("fields");

        return RestClient.builder()
                .url(TrelloSupport.getBaseUrl() + "/cards/" + cardId + TrelloSupport.getAuthQuery(context, true))
                .header("Content-Type", "application/json")
                .put(fields != null ? fields : Map.of())
                .execute();
    }
}
