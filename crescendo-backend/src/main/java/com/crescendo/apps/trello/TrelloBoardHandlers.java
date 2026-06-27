package com.crescendo.apps.trello;

import com.crescendo.execution.action.ActionContext;
import com.crescendo.execution.action.ActionMapping;
import com.crescendo.utils.RestClient;
import org.springframework.stereotype.Component;

import java.util.HashMap;
import java.util.Map;

/**
 * Trello Board handlers.
 */
@Component
public class TrelloBoardHandlers {

    @ActionMapping(appKey = "trello", actionKey = "trello:board:create")
    public Object createBoard(ActionContext context) throws Exception {
        String name = context.getString("name");
        String description = context.getString("description");
        
        Map<String, Object> body = new HashMap<>();
        body.put("name", name);
        if (description != null) body.put("desc", description);

        return RestClient.builder()
                .url(TrelloSupport.getBaseUrl() + "/boards" + TrelloSupport.getAuthQuery(context, true))
                .header("Content-Type", "application/json")
                .post(body)
                .execute();
    }

    @ActionMapping(appKey = "trello", actionKey = "trello:board:delete")
    public Object deleteBoard(ActionContext context) throws Exception {
        String boardId = context.getString("boardId");
        return RestClient.builder()
                .url(TrelloSupport.getBaseUrl() + "/boards/" + boardId + TrelloSupport.getAuthQuery(context, true))
                .delete()
                .execute();
    }

    @ActionMapping(appKey = "trello", actionKey = "trello:board:get")
    public Object getBoard(ActionContext context) throws Exception {
        String boardId = context.getString("boardId");
        return RestClient.builder()
                .url(TrelloSupport.getBaseUrl() + "/boards/" + boardId + TrelloSupport.getAuthQuery(context, true))
                .get()
                .execute();
    }

    @ActionMapping(appKey = "trello", actionKey = "trello:board:update")
    public Object updateBoard(ActionContext context) throws Exception {
        String boardId = context.getString("boardId");
        Map<String, Object> fields = context.getMap("fields");

        return RestClient.builder()
                .url(TrelloSupport.getBaseUrl() + "/boards/" + boardId + TrelloSupport.getAuthQuery(context, true))
                .header("Content-Type", "application/json")
                .put(fields != null ? fields : Map.of())
                .execute();
    }
}
