package com.crescendo.publicapi.common;

import java.nio.charset.StandardCharsets;
import java.util.Base64;

public class CursorUtils {
    
    private CursorUtils() {}

    /**
     * Encodes an offset into an opaque cursor.
     */
    public static String encodeOffset(int offset) {
        String payload = "offset:" + offset;
        return Base64.getUrlEncoder().withoutPadding().encodeToString(payload.getBytes(StandardCharsets.UTF_8));
    }

    /**
     * Decodes an opaque cursor into an offset. Returns 0 if invalid or null.
     */
    public static int decodeOffset(String cursor) {
        if (cursor == null || cursor.isBlank()) return 0;
        try {
            String decoded = new String(Base64.getUrlDecoder().decode(cursor), StandardCharsets.UTF_8);
            if (decoded.startsWith("offset:")) {
                return Integer.parseInt(decoded.substring("offset:".length()));
            }
        } catch (Exception e) {
            // Invalid cursor, just return 0 or throw bad request
        }
        return 0;
    }
}
