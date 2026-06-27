package com.crescendo.apps.trello;

import com.crescendo.execution.action.ActionContext;
import com.crescendo.execution.action.ActionMapping;
import com.crescendo.utils.RestClient;
import org.springframework.stereotype.Component;

import java.util.Map;

/**
 * Trello Checklist handlers.
 */
@Component
public class TrelloChecklistHandlers {

    @ActionMapping(appKey = "trello", actionKey = "trello:checklist:create")
    public Object createChecklist(ActionContext context) throws Exception {
        String cardId = context.getString("cardId");
        String name = context.getString("name");

        return RestClient.builder()
                .url(TrelloSupport.getBaseUrl() + "/checklists" + TrelloSupport.getAuthQuery(context, true))
                .header("Content-Type", "application/json")
                .post(Map.of("idCard", cardId, "name", name))
                .execute();
    }

    @ActionMapping(appKey = "trello", actionKey = "trello:checklist:delete")
    public Object deleteChecklist(ActionContext context) throws Exception {
        String cardId = context.getString("cardId");
        String checklistId = context.getString("checklistId");
        return RestClient.builder()
                .url(TrelloSupport.getBaseUrl() + "/cards/" + cardId + "/checklists/" + checklistId + TrelloSupport.getAuthQuery(context, true))
                .delete()
                .execute();
    }

    @ActionMapping(appKey = "trello", actionKey = "trello:checklist:get")
    public Object getChecklist(ActionContext context) throws Exception {
        String checklistId = context.getString("checklistId");
        return RestClient.builder()
                .url(TrelloSupport.getBaseUrl() + "/checklists/" + checklistId + TrelloSupport.getAuthQuery(context, true))
                .get()
                .execute();
    }

    @ActionMapping(appKey = "trello", actionKey = "trello:checklist:getAll")
    public Object getAllChecklists(ActionContext context) throws Exception {
        String cardId = context.getString("cardId");
        return RestClient.builder()
                .url(TrelloSupport.getBaseUrl() + "/cards/" + cardId + "/checklists" + TrelloSupport.getAuthQuery(context, true))
                .get()
                .execute();
    }
}
