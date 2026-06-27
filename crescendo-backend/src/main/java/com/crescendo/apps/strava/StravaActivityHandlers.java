package com.crescendo.apps.strava;

import com.crescendo.execution.action.ActionContext;
import com.crescendo.execution.action.ActionMapping;
import com.crescendo.utils.RestClient;
import org.springframework.stereotype.Component;

import java.util.HashMap;
import java.util.Map;

@Component
public class StravaActivityHandlers {

    private String getAuth(ActionContext context) {
        return "Bearer " + context.getCredential("accessToken");
    }

    @ActionMapping(appKey = "strava", actionKey = "strava:activity:create")
    public Object createActivity(ActionContext context) throws Exception {
        Map<String, Object> body = new HashMap<>();
        body.put("name", context.getString("name"));
        body.put("type", context.getString("type"));
        body.put("start_date_local", context.getString("startDate"));
        body.put("elapsed_time", context.getInt("elapsedTime"));
        
        String desc = context.getString("description");
        if (desc != null) body.put("description", desc);
        
        String distStr = context.getString("distance");
        if (distStr != null && !distStr.isBlank()) {
            body.put("distance", Double.parseDouble(distStr));
        }

        return RestClient.builder()
                .url("https://www.strava.com/api/v3/activities")
                .header("Authorization", getAuth(context))
                .header("Content-Type", "application/json")
                .post(body)
                .execute();
    }

    @ActionMapping(appKey = "strava", actionKey = "strava:activity:getMany")
    public Object getActivities(ActionContext context) throws Exception {
        return RestClient.builder()
                .url("https://www.strava.com/api/v3/athlete/activities")
                .header("Authorization", getAuth(context))
                .get()
                .execute();
    }

    @ActionMapping(appKey = "strava", actionKey = "strava:activity:update")
    public Object updateActivity(ActionContext context) throws Exception {
        String activityId = context.getString("activityId");
        
        Map<String, Object> body = new HashMap<>();
        String name = context.getString("name");
        if (name != null) body.put("name", name);
        String desc = context.getString("description");
        if (desc != null) body.put("description", desc);

        return RestClient.builder()
                .url("https://www.strava.com/api/v3/activities/" + activityId)
                .header("Authorization", getAuth(context))
                .header("Content-Type", "application/json")
                .put(body)
                .execute();
    }
}
