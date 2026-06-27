package com.crescendo.apps.trello;

import com.crescendo.execution.action.ActionContext;
import com.crescendo.execution.action.ActionMapping;
import com.crescendo.utils.RestClient;
import org.springframework.stereotype.Component;

import java.util.HashMap;
import java.util.Map;

/**
 * Trello Attachment handlers.
 */
@Component
public class TrelloAttachmentHandlers {

    @ActionMapping(appKey = "trello", actionKey = "trello:attachment:create")
    public Object createAttachment(ActionContext context) throws Exception {
        String cardId = context.getString("cardId");
        String url = context.getString("url");
        String name = context.getString("name");

        Map<String, Object> body = new HashMap<>();
        if (url != null) body.put("url", url);
        if (name != null) body.put("name", name);

        return RestClient.builder()
                .url(TrelloSupport.getBaseUrl() + "/cards/" + cardId + "/attachments" + TrelloSupport.getAuthQuery(context, true))
                .header("Content-Type", "application/json")
                .post(body)
                .execute();
    }

    @ActionMapping(appKey = "trello", actionKey = "trello:attachment:delete")
    public Object deleteAttachment(ActionContext context) throws Exception {
        String cardId = context.getString("cardId");
        String attachmentId = context.getString("attachmentId");
        return RestClient.builder()
                .url(TrelloSupport.getBaseUrl() + "/cards/" + cardId + "/attachments/" + attachmentId + TrelloSupport.getAuthQuery(context, true))
                .delete()
                .execute();
    }

    @ActionMapping(appKey = "trello", actionKey = "trello:attachment:get")
    public Object getAttachment(ActionContext context) throws Exception {
        String cardId = context.getString("cardId");
        String attachmentId = context.getString("attachmentId");
        return RestClient.builder()
                .url(TrelloSupport.getBaseUrl() + "/cards/" + cardId + "/attachments/" + attachmentId + TrelloSupport.getAuthQuery(context, true))
                .get()
                .execute();
    }

    @ActionMapping(appKey = "trello", actionKey = "trello:attachment:getAll")
    public Object getAllAttachments(ActionContext context) throws Exception {
        String cardId = context.getString("cardId");
        return RestClient.builder()
                .url(TrelloSupport.getBaseUrl() + "/cards/" + cardId + "/attachments" + TrelloSupport.getAuthQuery(context, true))
                .get()
                .execute();
    }
}
