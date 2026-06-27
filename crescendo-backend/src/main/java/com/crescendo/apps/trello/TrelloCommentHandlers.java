package com.crescendo.apps.trello;

import com.crescendo.execution.action.ActionContext;
import com.crescendo.execution.action.ActionMapping;
import com.crescendo.utils.RestClient;
import org.springframework.stereotype.Component;

import java.util.Map;

/**
 * Trello Card Comment handlers.
 */
@Component
public class TrelloCommentHandlers {

    @ActionMapping(appKey = "trello", actionKey = "trello:cardComment:create")
    public Object createCardComment(ActionContext context) throws Exception {
        String cardId = context.getString("cardId");
        String text = context.getString("text");

        return RestClient.builder()
                .url(TrelloSupport.getBaseUrl() + "/cards/" + cardId + "/actions/comments" + TrelloSupport.getAuthQuery(context, true))
                .header("Content-Type", "application/json")
                .post(Map.of("text", text))
                .execute();
    }

    @ActionMapping(appKey = "trello", actionKey = "trello:cardComment:delete")
    public Object deleteCardComment(ActionContext context) throws Exception {
        String cardId = context.getString("cardId");
        String commentId = context.getString("commentId");
        return RestClient.builder()
                .url(TrelloSupport.getBaseUrl() + "/cards/" + cardId + "/actions/" + commentId + "/comments" + TrelloSupport.getAuthQuery(context, true))
                .delete()
                .execute();
    }

    @ActionMapping(appKey = "trello", actionKey = "trello:cardComment:update")
    public Object updateCardComment(ActionContext context) throws Exception {
        String cardId = context.getString("cardId");
        String commentId = context.getString("commentId");
        String text = context.getString("text");

        return RestClient.builder()
                .url(TrelloSupport.getBaseUrl() + "/cards/" + cardId + "/actions/" + commentId + "/comments" + TrelloSupport.getAuthQuery(context, true))
                .header("Content-Type", "application/json")
                .put(Map.of("text", text))
                .execute();
    }
}
