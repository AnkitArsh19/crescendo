package com.crescendo.apps.trello;

import com.crescendo.execution.action.ActionContext;
import com.crescendo.execution.action.ActionMapping;
import com.crescendo.utils.RestClient;
import org.springframework.stereotype.Component;

import java.util.HashMap;
import java.util.Map;

/**
 * Trello Label handlers.
 */
@Component
public class TrelloLabelHandlers {

    @ActionMapping(appKey = "trello", actionKey = "trello:label:create")
    public Object createLabel(ActionContext context) throws Exception {
        String boardId = context.getString("boardId");
        String name = context.getString("name");
        String color = context.getString("color");

        Map<String, Object> body = new HashMap<>();
        body.put("idBoard", boardId);
        body.put("name", name);
        if (color != null) body.put("color", color);

        return RestClient.builder()
                .url(TrelloSupport.getBaseUrl() + "/labels" + TrelloSupport.getAuthQuery(context, true))
                .header("Content-Type", "application/json")
                .post(body)
                .execute();
    }

    @ActionMapping(appKey = "trello", actionKey = "trello:label:delete")
    public Object deleteLabel(ActionContext context) throws Exception {
        String labelId = context.getString("labelId");
        return RestClient.builder()
                .url(TrelloSupport.getBaseUrl() + "/labels/" + labelId + TrelloSupport.getAuthQuery(context, true))
                .delete()
                .execute();
    }

    @ActionMapping(appKey = "trello", actionKey = "trello:label:get")
    public Object getLabel(ActionContext context) throws Exception {
        String labelId = context.getString("labelId");
        return RestClient.builder()
                .url(TrelloSupport.getBaseUrl() + "/labels/" + labelId + TrelloSupport.getAuthQuery(context, true))
                .get()
                .execute();
    }

    @ActionMapping(appKey = "trello", actionKey = "trello:label:getAll")
    public Object getAllLabels(ActionContext context) throws Exception {
        String boardId = context.getString("boardId");
        return RestClient.builder()
                .url(TrelloSupport.getBaseUrl() + "/boards/" + boardId + "/labels" + TrelloSupport.getAuthQuery(context, true))
                .get()
                .execute();
    }

    @ActionMapping(appKey = "trello", actionKey = "trello:label:update")
    public Object updateLabel(ActionContext context) throws Exception {
        String labelId = context.getString("labelId");
        Map<String, Object> fields = context.getMap("fields");

        return RestClient.builder()
                .url(TrelloSupport.getBaseUrl() + "/labels/" + labelId + TrelloSupport.getAuthQuery(context, true))
                .header("Content-Type", "application/json")
                .put(fields != null ? fields : Map.of())
                .execute();
    }
}
