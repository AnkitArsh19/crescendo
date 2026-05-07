package com.crescendo.security.access;

/**
 * Immutable snapshot of the platform limits that apply to a given {@link AccessTier}.
 *
 * Each limit is a hard cap. A value of {@link Integer#MAX_VALUE} effectively means "unlimited".
 *
 * Limits are resolved once when a request arrives (via {@link AccessControlService}) and
 * passed to service methods that need to enforce them — keeping policy separate from
 * business logic.
 *
 * ┌──────────────────────┬───────────┬───────────┬───────────┬───────────┐
 * │       Limit          │  GUEST    │ UNVERIFIED│ STANDARD  │  ADMIN    │
 * ├──────────────────────┼───────────┼───────────┼───────────┼───────────┤
 * │ maxWorkflows         │     1     │     3     │    50     │ unlimited │
 * │ maxStepsPerWorkflow  │     5     │     5     │   100     │ unlimited │
 * │ canActivateWorkflow  │   true    │   true    │   true    │   true    │
 * │ canUseWebhooks       │   false   │   false   │   true    │   true    │
 * │ canManageConnections │   true    │   true    │   true    │   true    │
 * │ canExportWorkflows   │   false   │   false   │   true    │   true    │
 * └──────────────────────┴───────────┴───────────┴───────────┴───────────┘
 */
public record PlatformLimits(
        int maxWorkflows,
        int maxStepsPerWorkflow,
        boolean canActivateWorkflow,
        boolean canUseWebhooks,
        boolean canManageConnections,
        boolean canExportWorkflows
) {

    /// Guest tier — just enough to try the platform as a portfolio demo.
    private static final PlatformLimits GUEST_LIMITS = new PlatformLimits(
            1,     // one workflow to try the platform
            5,     // five steps — enough for a meaningful test
            true,  // can activate and execute their workflow
            false, // no webhooks
            true,  // can connect apps to see the integration flow
            false  // no export
    );

    /// Unverified registered user — slightly more generous than guest.
    private static final PlatformLimits UNVERIFIED_LIMITS = new PlatformLimits(
            3,     // three workflows to explore
            5,     // five steps per workflow
            true,  // can activate and execute
            false, // no webhooks until verified
            true,  // can connect apps
            false  // no export until verified
    );

    /// Standard verified-user tier — generous limits for normal usage.
    private static final PlatformLimits STANDARD_LIMITS = new PlatformLimits(
            50,    // up to 50 workflows
            100,   // 100 steps per workflow
            true,
            true,
            true,
            true
    );

    /// Admin tier — effectively no restrictions.
    private static final PlatformLimits ADMIN_LIMITS = new PlatformLimits(
            Integer.MAX_VALUE,
            Integer.MAX_VALUE,
            true,
            true,
            true,
            true
    );

    /**
     * Returns the correct limits for the given access tier.
     * GUEST and UNVERIFIED both resolve to the same LIMITED instance.
     */
    public static PlatformLimits forTier(AccessTier tier) {
        return switch (tier) {
            case ADMIN      -> ADMIN_LIMITS;
            case STANDARD   -> STANDARD_LIMITS;
            case UNVERIFIED -> UNVERIFIED_LIMITS;
            case GUEST      -> GUEST_LIMITS;
        };
    }
}
