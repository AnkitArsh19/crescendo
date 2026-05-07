package com.crescendo.security.access;

import com.crescendo.enums.UserRole;

/**
 * Represents the effective access level of a user on the platform.
 * Determined by combining their role with email-verification status.
 *
 * Resolution order (highest to lowest):
 *   ADMIN  → role == ADMIN
 *   STANDARD → role == USER  && emailVerified
 *   UNVERIFIED → role == USER  && !emailVerified
 *   GUEST  → role == GUEST  (unauthenticated trial user)
 *
 * GUEST and UNVERIFIED share the same limits (isLimited() == true)
 * so they can be treated uniformly for access-gate checks.
 */
public enum AccessTier {

    /// Full platform access — no restrictions.
    ADMIN,

    /// Verified registered user — standard feature set.
    STANDARD,

    /// Registered but email not yet verified — treated the same as GUEST for limits.
    UNVERIFIED,

    /// Unauthenticated trial user — most restricted tier.
    GUEST;

    // -------------------------------------------------------------------------

    /**
     * Resolves the effective tier from a user's role and email-verification flag.
     * Called by AccessControlService to classify any authenticated principal.
     */
    public static AccessTier resolve(UserRole role, boolean emailVerified) {
        if (role == UserRole.ADMIN) return ADMIN;
        if (role == UserRole.GUEST) return GUEST;
        // role == USER
        return emailVerified ? STANDARD : UNVERIFIED;
    }

    /**
     * Returns true for tiers whose capabilities are restricted (GUEST + UNVERIFIED).
     * Convenience predicate so callers don't need to enumerate both tiers individually.
     */
    public boolean isLimited() {
        return this == GUEST || this == UNVERIFIED;
    }

    /**
     * Returns true only for fully verified registered users or admins.
     */
    public boolean hasFullAccess() {
        return this == STANDARD || this == ADMIN;
    }
}
