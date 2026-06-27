package com.crescendo.apps.todoist;

import com.crescendo.execution.action.ActionContext;
import com.crescendo.execution.action.ActionMapping;
import com.crescendo.utils.RestClient;
import org.springframework.stereotype.Component;

import java.util.HashMap;
import java.util.Map;

/**
 * Todoist Reminder handlers (via Sync API).
 */
@Component
public class TodoistReminderHandlers {

    private String getSyncBaseUrl() {
        return "https://api.todoist.com/sync/v9";
    }

    private String getAuth(ActionContext context) {
        return "Bearer " + context.getCredential("apiKey");
    }

    @ActionMapping(appKey = "todoist", actionKey = "todoist:reminder:create")
    public Object createReminder(ActionContext context) throws Exception {
        String taskId = context.getString("taskId");
        Integer minuteOffset = context.getInt("minuteOffset");

        Map<String, Object> command = new HashMap<>();
        command.put("type", "reminder_add");
        command.put("uuid", java.util.UUID.randomUUID().toString());
        command.put("temp_id", java.util.UUID.randomUUID().toString());

        Map<String, Object> args = new HashMap<>();
        args.put("item_id", taskId);
        if (minuteOffset != null) {
            args.put("minute_offset", minuteOffset);
        }
        command.put("args", args);

        return RestClient.builder()
                .url(getSyncBaseUrl() + "/sync")
                .header("Authorization", getAuth(context))
                .header("Content-Type", "application/json")
                .post(Map.of("commands", java.util.List.of(command)))
                .execute();
    }

    @ActionMapping(appKey = "todoist", actionKey = "todoist:reminder:update")
    public Object updateReminder(ActionContext context) throws Exception {
        String reminderId = context.getString("reminderId");
        Integer minuteOffset = context.getInt("minuteOffset");

        Map<String, Object> command = new HashMap<>();
        command.put("type", "reminder_update");
        command.put("uuid", java.util.UUID.randomUUID().toString());

        Map<String, Object> args = new HashMap<>();
        args.put("id", reminderId);
        if (minuteOffset != null) {
            args.put("minute_offset", minuteOffset);
        }
        command.put("args", args);

        return RestClient.builder()
                .url(getSyncBaseUrl() + "/sync")
                .header("Authorization", getAuth(context))
                .header("Content-Type", "application/json")
                .post(Map.of("commands", java.util.List.of(command)))
                .execute();
    }

    @ActionMapping(appKey = "todoist", actionKey = "todoist:reminder:delete")
    public Object deleteReminder(ActionContext context) throws Exception {
        String reminderId = context.getString("reminderId");

        Map<String, Object> command = new HashMap<>();
        command.put("type", "reminder_delete");
        command.put("uuid", java.util.UUID.randomUUID().toString());
        command.put("args", Map.of("id", reminderId));

        return RestClient.builder()
                .url(getSyncBaseUrl() + "/sync")
                .header("Authorization", getAuth(context))
                .header("Content-Type", "application/json")
                .post(Map.of("commands", java.util.List.of(command)))
                .execute();
    }

    @ActionMapping(appKey = "todoist", actionKey = "todoist:reminder:getAll")
    public Object getAllReminders(ActionContext context) throws Exception {
        return RestClient.builder()
                .url(getSyncBaseUrl() + "/sync?sync_token=*&resource_types=[\"reminders\"]")
                .header("Authorization", getAuth(context))
                .get()
                .execute();
    }
}
