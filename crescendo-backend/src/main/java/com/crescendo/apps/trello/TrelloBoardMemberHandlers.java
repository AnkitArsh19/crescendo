package com.crescendo.apps.trello;

import com.crescendo.execution.action.ActionContext;
import com.crescendo.execution.action.ActionMapping;
import com.crescendo.utils.RestClient;
import org.springframework.stereotype.Component;

import java.util.Map;

/**
 * Trello Board Member handlers.
 */
@Component
public class TrelloBoardMemberHandlers {

    @ActionMapping(appKey = "trello", actionKey = "trello:boardMember:add")
    public Object addBoardMember(ActionContext context) throws Exception {
        String boardId = context.getString("boardId");
        String memberId = context.getString("memberId");
        String type = context.getString("type", "normal");

        return RestClient.builder()
                .url(TrelloSupport.getBaseUrl() + "/boards/" + boardId + "/members/" + memberId + TrelloSupport.getAuthQuery(context, true))
                .header("Content-Type", "application/json")
                .put(Map.of("type", type))
                .execute();
    }

    @ActionMapping(appKey = "trello", actionKey = "trello:boardMember:get")
    public Object getBoardMember(ActionContext context) throws Exception {
        String boardId = context.getString("boardId");
        String memberId = context.getString("memberId");
        return RestClient.builder()
                .url(TrelloSupport.getBaseUrl() + "/boards/" + boardId + "/members/" + memberId + TrelloSupport.getAuthQuery(context, true))
                .get()
                .execute();
    }

    @ActionMapping(appKey = "trello", actionKey = "trello:boardMember:getAll")
    public Object getAllBoardMembers(ActionContext context) throws Exception {
        String boardId = context.getString("boardId");
        return RestClient.builder()
                .url(TrelloSupport.getBaseUrl() + "/boards/" + boardId + "/members" + TrelloSupport.getAuthQuery(context, true))
                .get()
                .execute();
    }

    @ActionMapping(appKey = "trello", actionKey = "trello:boardMember:remove")
    public Object removeBoardMember(ActionContext context) throws Exception {
        String boardId = context.getString("boardId");
        String memberId = context.getString("memberId");
        return RestClient.builder()
                .url(TrelloSupport.getBaseUrl() + "/boards/" + boardId + "/members/" + memberId + TrelloSupport.getAuthQuery(context, true))
                .delete()
                .execute();
    }
}
