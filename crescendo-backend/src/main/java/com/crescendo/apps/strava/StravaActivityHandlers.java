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
        
        String name = context.getString("name");
        body.put("name", (name != null && !name.isBlank()) ? name : "Activity");
        
        String type = context.getString("type");
        body.put("type", (type != null && !type.isBlank()) ? type : "Run");
        
        String startDate = context.getString("startDate");
        if (startDate == null || startDate.isBlank() || startDate.contains("{{now") || startDate.contains("{{today")) {
            startDate = java.time.Instant.now().toString();
        }
        body.put("start_date_local", startDate);
        
        Integer elapsed = context.getInt("elapsedTime");
        if (elapsed == null) elapsed = context.getInt("duration");
        if (elapsed == null) elapsed = 900;
        body.put("elapsed_time", elapsed);
        
        String desc = context.getString("description");
        if (desc != null && !desc.isBlank()) body.put("description", desc);
        
        String distStr = context.getString("distance");
        if (distStr != null && !distStr.isBlank()) {
            try {
                body.put("distance", Double.parseDouble(distStr));
            } catch (NumberFormatException ignored) {}
        }

        try {
            return RestClient.builder()
                    .url("https://www.strava.com/api/v3/activities")
                    .header("Authorization", getAuth(context))
                    .header("Content-Type", "application/json")
                    .post(body)
                    .execute();
        } catch (Exception e) {
            String msg = e.getMessage() != null ? e.getMessage() : "";
            if (msg.contains("Inactive") || msg.contains("403")) {
                throw new RuntimeException("Strava API Application is Inactive. Please go to your Strava Developer Settings (https://www.strava.com/settings/api), upload an Application Icon, and save to activate your Strava API app.", e);
            }
            throw e;
        }
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
