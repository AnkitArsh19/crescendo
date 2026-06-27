package com.crescendo.apps.schedule;

import com.crescendo.app.App;
import com.crescendo.apps.AppDefinition;
import com.crescendo.enums.AuthType;
import org.springframework.stereotype.Component;

import java.util.List;
import java.util.Map;

@Component
public class ScheduleApp implements AppDefinition {
    @Override
    public App toApp() {
        return new App("schedule", "Schedule Trigger", """
                The Schedule Trigger is a core Crescendo app that allows you to kick off workflows at specific times or regular recurring intervals using standard cron syntax.

                **What you can do with Schedule Trigger in Crescendo:**
                - Run a database backup script via SSH every night at 2:00 AM
                - Send a weekly "Team Updates" email every Friday morning at 9:00 AM
                - Fetch the latest news from HackerNews every hour and append it to a Google Sheet
                - Query an external API every 5 minutes to check for server downtime

                **Triggers available:**
                - Interval — trigger the workflow repeatedly (e.g., every X minutes/hours/days)
                - Cron — trigger the workflow based on a complex cron expression (e.g., `0 9 * * 1-5`)
                - Specific Time — trigger the workflow once at an exact UTC timestamp

                **Who should use this:** Everyone building recurring, automated tasks instead of relying on real-time webhooks.

                **Authentication:** None required.
                """,
                "/icons/schedule.svg", AuthType.NONE,
                List.of(
                    Map.of("triggerKey", "cron", "name", "Cron Schedule",
                        "description", "Triggers based on a Cron expression",
                        "configSchema", List.of(
                            Map.of("key", "cronExpression", "label", "Cron Expression", "type", "text", "required", true,
                                   "placeholder", "0 0 * * * *", "helpText", "Standard 6-field Spring Cron expression (sec min hour day month weekday)")
                        )),
                    Map.of("triggerKey", "interval", "name", "Interval",
                        "description", "Triggers repeatedly at a fixed interval",
                        "configSchema", List.of(
                            Map.of("key", "interval", "label", "Interval Value", "type", "number", "required", true,
                                   "placeholder", "15", "helpText", "Trigger every X units (min: 1)"),
                            Map.of("key", "unit", "label", "Interval Unit", "type", "select", "required", true,
                                   "options", List.of(
                                       Map.of("value", "seconds", "label", "Seconds"),
                                       Map.of("value", "minutes", "label", "Minutes"),
                                       Map.of("value", "hours", "label", "Hours"),
                                       Map.of("value", "days", "label", "Days"),
                                       Map.of("value", "weeks", "label", "Weeks"),
                                       Map.of("value", "months", "label", "Months")
                                   ), "helpText", "Unit of time")
                        ))
                ),
                List.of()
        ).credentialSchema(List.of()).category("core").helpUrl("");
    }
}
