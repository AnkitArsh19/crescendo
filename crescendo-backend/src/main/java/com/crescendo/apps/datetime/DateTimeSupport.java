package com.crescendo.apps.datetime;

import java.time.Instant;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.time.ZoneId;
import java.time.ZonedDateTime;
import java.time.format.DateTimeFormatter;

final class DateTimeSupport {

    private DateTimeSupport() {
    }

    static ZonedDateTime parseDateTime(Object value, ZoneId zoneId) {
        if (value == null) {
            return ZonedDateTime.now(zoneId);
        }

        String raw = String.valueOf(value).trim();
        if (raw.isBlank() || "now".equalsIgnoreCase(raw)) {
            return ZonedDateTime.now(zoneId);
        }

        try {
            return ZonedDateTime.parse(raw);
        } catch (Exception ignored) {
        }
        try {
            return Instant.parse(raw).atZone(zoneId);
        } catch (Exception ignored) {
        }
        try {
            return LocalDateTime.parse(raw).atZone(zoneId);
        } catch (Exception ignored) {
        }
        try {
            return LocalDate.parse(raw).atStartOfDay(zoneId);
        } catch (Exception ignored) {
        }
        try {
            long epoch = Long.parseLong(raw);
            if (raw.length() <= 10) {
                return Instant.ofEpochSecond(epoch).atZone(zoneId);
            }
            return Instant.ofEpochMilli(epoch).atZone(zoneId);
        } catch (Exception ignored) {
        }

        throw new IllegalArgumentException("Unsupported date value: " + raw);
    }

    static ZoneId zone(Object value) {
        if (value == null || String.valueOf(value).isBlank()) {
            return ZoneId.of("UTC");
        }
        return ZoneId.of(String.valueOf(value));
    }

    static String format(ZonedDateTime dateTime, Object pattern) {
        if (pattern == null || String.valueOf(pattern).isBlank() || "ISO".equalsIgnoreCase(String.valueOf(pattern))) {
            return dateTime.format(DateTimeFormatter.ISO_OFFSET_DATE_TIME);
        }
        return dateTime.format(DateTimeFormatter.ofPattern(String.valueOf(pattern)));
    }

    static int intValue(Object value, int fallback) {
        if (value instanceof Number n) return n.intValue();
        try {
            return value == null ? fallback : Integer.parseInt(String.valueOf(value));
        } catch (NumberFormatException e) {
            return fallback;
        }
    }
}
