package com.crescendo.apps.datetime;

import com.crescendo.execution.action.ActionContext;
import com.crescendo.execution.action.ActionMapping;
import org.springframework.stereotype.Component;

import java.time.Duration;
import java.time.ZoneId;
import java.time.ZonedDateTime;
import java.time.format.DateTimeFormatter;
import java.time.temporal.ChronoUnit;
import java.util.LinkedHashMap;
import java.util.Map;

/**
 * Production DateTime action handlers for appKey="dateTime".
 */
@Component
public class DateTimeHandlers {

    @ActionMapping(appKey = "dateTime", actionKey = "dateTime:getCurrentDate")
    public Object getCurrentDate(ActionContext context) throws Exception {
        String outputFieldName = context.getString("outputFieldName");
        if (outputFieldName == null || outputFieldName.isBlank()) outputFieldName = "date";

        Map<String, Object> options = context.getMap("options");
        String timezone = options != null ? String.valueOf(options.getOrDefault("timezone", "UTC")) : "UTC";
        ZoneId zone = DateTimeSupport.zone(timezone);
        ZonedDateTime now = ZonedDateTime.now(zone);

        boolean includeTime = context.getBoolean("includeTime", true);
        String formatted = includeTime
                ? now.format(DateTimeFormatter.ISO_OFFSET_DATE_TIME)
                : now.format(DateTimeFormatter.ISO_LOCAL_DATE);

        Map<String, Object> result = new LinkedHashMap<>();
        result.put(outputFieldName, formatted);
        result.put("timestamp", now.toInstant().toEpochMilli());
        result.put("timezone", zone.getId());
        return result;
    }

    @ActionMapping(appKey = "dateTime", actionKey = "dateTime:addToDate")
    public Object addToDate(ActionContext context) throws Exception {
        String outputFieldName = context.getString("outputFieldName");
        if (outputFieldName == null || outputFieldName.isBlank()) outputFieldName = "date";

        String dateStr = context.getString("magnitude");
        String timeUnit = context.getString("timeUnit");
        int duration = context.getInt("duration", 1);

        ZoneId zone = ZoneId.of("UTC");
        ZonedDateTime dt = DateTimeSupport.parseDateTime(dateStr, zone);
        ZonedDateTime modified = applyDuration(dt, timeUnit, duration);

        Map<String, Object> result = new LinkedHashMap<>();
        result.put(outputFieldName, modified.format(DateTimeFormatter.ISO_OFFSET_DATE_TIME));
        result.put("timestamp", modified.toInstant().toEpochMilli());
        return result;
    }

    @ActionMapping(appKey = "dateTime", actionKey = "dateTime:subtractFromDate")
    public Object subtractFromDate(ActionContext context) throws Exception {
        String outputFieldName = context.getString("outputFieldName");
        if (outputFieldName == null || outputFieldName.isBlank()) outputFieldName = "date";

        String dateStr = context.getString("magnitude");
        String timeUnit = context.getString("timeUnit");
        int duration = context.getInt("duration", 1);

        ZoneId zone = ZoneId.of("UTC");
        ZonedDateTime dt = DateTimeSupport.parseDateTime(dateStr, zone);
        ZonedDateTime modified = applyDuration(dt, timeUnit, -duration);

        Map<String, Object> result = new LinkedHashMap<>();
        result.put(outputFieldName, modified.format(DateTimeFormatter.ISO_OFFSET_DATE_TIME));
        result.put("timestamp", modified.toInstant().toEpochMilli());
        return result;
    }

    @ActionMapping(appKey = "dateTime", actionKey = "dateTime:formatDate")
    public Object formatDate(ActionContext context) throws Exception {
        String outputFieldName = context.getString("outputFieldName");
        if (outputFieldName == null || outputFieldName.isBlank()) outputFieldName = "formattedDate";

        String dateStr = context.getString("date");
        String format = context.getString("format");

        ZoneId zone = ZoneId.of("UTC");
        ZonedDateTime dt = DateTimeSupport.parseDateTime(dateStr, zone);
        String formatted = DateTimeSupport.format(dt, format);

        Map<String, Object> result = new LinkedHashMap<>();
        result.put(outputFieldName, formatted);
        result.put("rawDate", dt.format(DateTimeFormatter.ISO_OFFSET_DATE_TIME));
        return result;
    }

    @ActionMapping(appKey = "dateTime", actionKey = "dateTime:roundDate")
    public Object roundDate(ActionContext context) throws Exception {
        String outputFieldName = context.getString("outputFieldName");
        if (outputFieldName == null || outputFieldName.isBlank()) outputFieldName = "roundedDate";

        String dateStr = context.getString("date");
        String mode = context.getString("mode"); // "roundUp", "roundDown"
        String to = context.getString("to");     // "hour", "day", "month", "minute"

        ZoneId zone = ZoneId.of("UTC");
        ZonedDateTime dt = DateTimeSupport.parseDateTime(dateStr, zone);

        ZonedDateTime rounded = switch (to != null ? to.toLowerCase() : "day") {
            case "minute" -> dt.truncatedTo(ChronoUnit.MINUTES);
            case "hour"   -> dt.truncatedTo(ChronoUnit.HOURS);
            case "month"  -> dt.withDayOfMonth(1).truncatedTo(ChronoUnit.DAYS);
            case "year"   -> dt.withDayOfYear(1).truncatedTo(ChronoUnit.DAYS);
            default       -> dt.truncatedTo(ChronoUnit.DAYS);
        };

        if ("roundUp".equalsIgnoreCase(mode) && !rounded.isEqual(dt)) {
            rounded = switch (to != null ? to.toLowerCase() : "day") {
                case "minute" -> rounded.plusMinutes(1);
                case "hour"   -> rounded.plusHours(1);
                case "month"  -> rounded.plusMonths(1);
                case "year"   -> rounded.plusYears(1);
                default       -> rounded.plusDays(1);
            };
        }

        Map<String, Object> result = new LinkedHashMap<>();
        result.put(outputFieldName, rounded.format(DateTimeFormatter.ISO_OFFSET_DATE_TIME));
        return result;
    }

    @ActionMapping(appKey = "dateTime", actionKey = "dateTime:getTimeBetweenDates")
    public Object getTimeBetweenDates(ActionContext context) throws Exception {
        String outputFieldName = context.getString("outputFieldName");
        if (outputFieldName == null || outputFieldName.isBlank()) outputFieldName = "difference";

        String startStr = context.getString("startDate");
        String endStr = context.getString("endDate");
        String units = context.getString("units");

        ZoneId zone = ZoneId.of("UTC");
        ZonedDateTime start = DateTimeSupport.parseDateTime(startStr, zone);
        ZonedDateTime end = DateTimeSupport.parseDateTime(endStr, zone);

        Duration duration = Duration.between(start, end);
        long diff = switch (units != null ? units.toLowerCase() : "seconds") {
            case "milliseconds", "ms" -> duration.toMillis();
            case "minutes", "m"       -> duration.toMinutes();
            case "hours", "h"         -> duration.toHours();
            case "days", "d"          -> duration.toDays();
            default                   -> duration.toSeconds();
        };

        Map<String, Object> result = new LinkedHashMap<>();
        result.put(outputFieldName, diff);
        result.put("durationSeconds", duration.toSeconds());
        result.put("durationMillis", duration.toMillis());
        return result;
    }

    @ActionMapping(appKey = "dateTime", actionKey = "dateTime:extractDate")
    public Object extractDate(ActionContext context) throws Exception {
        String outputFieldName = context.getString("outputFieldName");
        if (outputFieldName == null || outputFieldName.isBlank()) outputFieldName = "value";

        String dateStr = context.getString("date");
        String part = context.getString("part");

        ZoneId zone = ZoneId.of("UTC");
        ZonedDateTime dt = DateTimeSupport.parseDateTime(dateStr, zone);

        Object val = switch (part != null ? part.toLowerCase() : "year") {
            case "month"        -> dt.getMonthValue();
            case "monthname"    -> dt.getMonth().name();
            case "day", "date"  -> dt.getDayOfMonth();
            case "dayofweek"    -> dt.getDayOfWeek().name();
            case "hour"         -> dt.getHour();
            case "minute"       -> dt.getMinute();
            case "second"       -> dt.getSecond();
            case "millisecond"  -> dt.getNano() / 1_000_000;
            default             -> dt.getYear();
        };

        Map<String, Object> result = new LinkedHashMap<>();
        result.put(outputFieldName, val);
        return result;
    }

    private static ZonedDateTime applyDuration(ZonedDateTime dt, String unit, int amount) {
        if (unit == null) return dt.plusDays(amount);
        return switch (unit.toLowerCase().trim()) {
            case "seconds", "second", "s" -> dt.plusSeconds(amount);
            case "minutes", "minute", "m" -> dt.plusMinutes(amount);
            case "hours", "hour", "h"     -> dt.plusHours(amount);
            case "weeks", "week", "w"     -> dt.plusWeeks(amount);
            case "months", "month", "mo"  -> dt.plusMonths(amount);
            case "years", "year", "y"     -> dt.plusYears(amount);
            default                       -> dt.plusDays(amount);
        };
    }
}
