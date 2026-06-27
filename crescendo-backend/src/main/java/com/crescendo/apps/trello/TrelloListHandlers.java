package com.crescendo.apps.trello;

import com.crescendo.execution.action.ActionContext;
import com.crescendo.execution.action.ActionMapping;
import com.crescendo.utils.RestClient;
import org.springframework.stereotype.Component;

import java.util.Map;

/**
 * Trello List handlers.
 */
@Component
public class TrelloListHandlers {

    @ActionMapping(appKey = "trello", actionKey = "trello:list:create")
    public Object createList(ActionContext context) throws Exception {
        String boardId = context.getString("boardId");
        String name = context.getString("name");

        return RestClient.builder()
                .url(TrelloSupport.getBaseUrl() + "/lists" + TrelloSupport.getAuthQuery(context, true))
                .header("Content-Type", "application/json")
                .post(Map.of("idBoard", boardId, "name", name))
                .execute();
    }

    @ActionMapping(appKey = "trello", actionKey = "trello:list:get")
    public Object getList(ActionContext context) throws Exception {
        String listId = context.getString("listId");
        return RestClient.builder()
                .url(TrelloSupport.getBaseUrl() + "/lists/" + listId + TrelloSupport.getAuthQuery(context, true))
                .get()
                .execute();
    }

    @ActionMapping(appKey = "trello", actionKey = "trello:list:getAll")
    public Object getAllLists(ActionContext context) throws Exception {
        String boardId = context.getString("boardId");
        return RestClient.builder()
                .url(TrelloSupport.getBaseUrl() + "/boards/" + boardId + "/lists" + TrelloSupport.getAuthQuery(context, true))
                .get()
                .execute();
    }

    @ActionMapping(appKey = "trello", actionKey = "trello:list:update")
    public Object updateList(ActionContext context) throws Exception {
        String listId = context.getString("listId");
        Map<String, Object> fields = context.getMap("fields");

        return RestClient.builder()
                .url(TrelloSupport.getBaseUrl() + "/lists/" + listId + TrelloSupport.getAuthQuery(context, true))
                .header("Content-Type", "application/json")
                .put(fields != null ? fields : Map.of())
                .execute();
    }
}
