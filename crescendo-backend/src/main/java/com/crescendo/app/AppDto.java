package com.crescendo.app;

import java.io.Serializable;
import java.util.List;
import java.util.Map;

/**
 * Request and response DTOs for the app catalog.
 * The app catalog is entirely read-only — apps are seeded via migrations.
 */
public class AppDto {

    /**
     * Summary view for app listing — includes credential schema and metadata
     * so the frontend can render structured connection forms.
     */
    public record AppSummaryResponse(
            String appKey,
            String name,
            String description,
            String logoUrl,
            String authType,
            String altAuthType,
            List<Map<String, Object>> credentialSchema,
            String category,
            String helpUrl,
            boolean internal
    ) implements Serializable {}

    /**
     * Detail view including triggers and actions definitions.
     */
    public record AppDetailResponse(
            String appKey,
            String name,
            String description,
            String logoUrl,
            String authType,
            String altAuthType,
            List<Map<String, Object>> triggers,
            List<Map<String, Object>> actions,
            List<Map<String, Object>> credentialSchema,
            String category,
            String helpUrl,
            boolean internal
        ) implements Serializable {}
}
