package com.crescendo.apps.calendly;

import com.crescendo.execution.action.ActionContext;
import com.crescendo.execution.action.ActionMapping;
import com.crescendo.utils.RestClient;
import org.springframework.stereotype.Component;

@Component
public class CalendlyHandlers {

    private String getBaseUrl() {
        return "https://api.calendly.com";
    }

    private String getAuth(ActionContext context) {
        String token = context.getCredential("accessToken");
        if (token == null || token.isBlank()) {
            token = context.getCredential("personalToken");
        }
        if (token == null || token.isBlank()) {
            token = context.getCredential("token");
        }
        return "Bearer " + token;
    }

    @ActionMapping(appKey = "calendly", actionKey = "calendly:event:getMany")
    public Object getEvents(ActionContext context) throws Exception {
        return RestClient.builder()
                .url(getBaseUrl() + "/scheduled_events")
                .header("Authorization", getAuth(context))
                .get()
                .execute();
    }

    @ActionMapping(appKey = "calendly", actionKey = "calendly:eventInvitee:getMany")
    public Object getEventInvitees(ActionContext context) throws Exception {
        String eventUuid = context.getString("eventUuid");
        return RestClient.builder()
                .url(getBaseUrl() + "/scheduled_events/" + eventUuid + "/invitees")
                .header("Authorization", getAuth(context))
                .get()
                .execute();
    }
}
