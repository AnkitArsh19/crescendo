package com.crescendo.apps.strava;

import com.crescendo.execution.action.ActionContext;
import com.crescendo.execution.action.ActionMapping;
import com.crescendo.utils.RestClient;
import org.springframework.stereotype.Component;

@Component
public class StravaAthleteHandlers {

    private String getAuth(ActionContext context) {
        return "Bearer " + context.getCredential("accessToken");
    }

    @ActionMapping(appKey = "strava", actionKey = "strava:athlete:get")
    public Object getAthlete(ActionContext context) throws Exception {
        return RestClient.builder()
                .url("https://www.strava.com/api/v3/athlete")
                .header("Authorization", getAuth(context))
                .get()
                .execute();
    }
}
