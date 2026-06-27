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
     *
     * <p>{@code hasPlatformKey} — when {@code true}, the frontend should show a
     * "Use Crescendo's Key" option so the user can skip providing personal credentials.
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
            boolean internal,
            boolean hasPlatformKey
    ) implements Serializable {}

    /**
     * Detail view including triggers and actions definitions.
     *
     * <p>{@code hasPlatformKey} — same as in summary; the frontend uses this
     * to conditionally render the credential-source toggle in the node config panel.
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
            boolean internal,
            boolean hasPlatformKey
        ) implements Serializable {}
}

