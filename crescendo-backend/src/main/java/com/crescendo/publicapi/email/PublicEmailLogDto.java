package com.crescendo.publicapi.email;

import com.crescendo.enums.EmailStatus;
import java.time.Instant;
import java.util.Map;

public class PublicEmailLogDto {

    public record EmailLogResponse(
            String id,
            String from,
            String to,
            String subject,
            EmailStatus status,
            Instant createdAt,
            Map<String, String> tags) {
    }

}
