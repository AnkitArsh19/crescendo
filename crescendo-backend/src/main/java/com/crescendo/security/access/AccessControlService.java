package com.crescendo.security.access;

import com.crescendo.enums.UserRole;
import com.crescendo.security.AppUserDetails;
import com.crescendo.workflow.workflow_command.Workflow_commandRepository;
import org.springframework.http.HttpStatus;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.stereotype.Service;
import org.springframework.web.server.ResponseStatusException;

import java.util.UUID;

/**
 * Central service for platform access-control decisions.
 *
 * Resolves a user's {@link AccessTier} from their role + email-verification status
 * and enforces the corresponding {@link PlatformLimits}.
 *
 * Usage from controllers/services:
 *   accessControl.requireFullAccess();          // throws 403 if limited
 *   accessControl.enforceWorkflowLimit(userId); // throws 403 if at capacity
 *   PlatformLimits limits = accessControl.currentLimits(); // inspect limits
 *
 * Usage in @PreAuthorize SpEL expressions (bean name = "accessControl"):
 *   @PreAuthorize("@accessControl.isFullAccess()")
 *   @PreAuthorize("@accessControl.isVerifiedUser()")
 */
@Service("accessControl")
public class AccessControlService {

    private final Workflow_commandRepository workflowRepo;

    public AccessControlService(Workflow_commandRepository workflowRepo) {
        this.workflowRepo = workflowRepo;
    }

    // -------------------------------------------------------------------------
    // TIER RESOLUTION
    // -------------------------------------------------------------------------

    /**
     * Resolves the access tier of the currently authenticated user.
     * Falls back to GUEST if no authentication is present (anonymous request).
     */
    public AccessTier currentTier() {
        AppUserDetails principal = currentPrincipal();
        if (principal == null) return AccessTier.GUEST;
        return AccessTier.resolve(principal.getRole(), principal.isEmailVerified());
    }

    /**
     * Resolves the tier for an explicit role + emailVerified combination.
     * Useful when the caller already has the user entity (e.g., inside a service method).
     */
    public AccessTier tierFor(UserRole role, boolean emailVerified) {
        return AccessTier.resolve(role, emailVerified);
    }

    /**
     * Returns the platform limits that apply to the currently authenticated user.
     */
    public PlatformLimits currentLimits() {
        return PlatformLimits.forTier(currentTier());
    }

    /**
     * Returns the platform limits for a given tier.
     */
    public PlatformLimits limitsFor(AccessTier tier) {
        return PlatformLimits.forTier(tier);
    }

    // -------------------------------------------------------------------------
    // SPEL-FRIENDLY BOOLEAN CHECKS (for @PreAuthorize)
    // -------------------------------------------------------------------------

    /**
     * Returns true if the current user has full (non-limited) access.
     * Usage: @PreAuthorize("@accessControl.isFullAccess()")
     */
    public boolean isFullAccess() {
        return currentTier().hasFullAccess();
    }

    /**
     * Returns true if the current user is a verified registered user (not ADMIN).
     * Usage: @PreAuthorize("@accessControl.isVerifiedUser()")
     */
    public boolean isVerifiedUser() {
        return currentTier() == AccessTier.STANDARD;
    }

    /**
     * Returns true if the current user is an admin.
     * Usage: @PreAuthorize("@accessControl.isAdmin()")
     */
    public boolean isAdmin() {
        return currentTier() == AccessTier.ADMIN;
    }

    /**
     * Returns true if the current user is limited (GUEST or UNVERIFIED).
     * Usage: @PreAuthorize("!@accessControl.isLimited()")
     */
    public boolean isLimited() {
        return currentTier().isLimited();
    }

    // -------------------------------------------------------------------------
    // IMPERATIVE ENFORCEMENT (throw 403 on violation)
    // -------------------------------------------------------------------------

    /**
     * Throws 403 FORBIDDEN if the current user doesn't have full access.
     * Use for endpoints that require a verified email (e.g., managing connections, webhooks).
     */
    public void requireFullAccess() {
        if (!currentTier().hasFullAccess()) {
            throw new ResponseStatusException(HttpStatus.FORBIDDEN,
                    "This feature requires a verified email address");
        }
    }

    /**
     * Throws 403 FORBIDDEN if the current user's tier doesn't allow workflow activation.
     */
    public void requireCanActivateWorkflow() {
        if (!currentLimits().canActivateWorkflow()) {
            throw new ResponseStatusException(HttpStatus.FORBIDDEN,
                    "Workflow activation requires a verified email address");
        }
    }

    /**
     * Checks whether the user has room to create another workflow.
     * Throws 403 with a clear message if they've reached the limit for their tier.
     *
     * @param userId the ID of the authenticated user creating the workflow
     */
    public void enforceWorkflowLimit(UUID userId) {
        PlatformLimits limits = currentLimits();
        long current = workflowRepo.countByUser_IdAndDeletedAtIsNull(userId);
        if (current >= limits.maxWorkflows()) {
            throw new ResponseStatusException(HttpStatus.FORBIDDEN,
                    "Workflow limit reached (" + limits.maxWorkflows() + "). "
                            + (currentTier().isLimited()
                            ? "Verify your email to unlock more workflows."
                            : "You have reached the maximum for your plan."));
        }
    }

    /**
     * Same as {@link #enforceWorkflowLimit(UUID)} but for guest sessions
     * identified by a session ID instead of a user account.
     *
     * @param guestSessionId the guest's session identifier
     */
    public void enforceGuestWorkflowLimit(String guestSessionId) {
        PlatformLimits limits = PlatformLimits.forTier(AccessTier.GUEST);
        long current = workflowRepo.countByGuestSessionId_ValueAndDeletedAtIsNull(guestSessionId);
        if (current >= limits.maxWorkflows()) {
            throw new ResponseStatusException(HttpStatus.FORBIDDEN,
                    "Guest workflow limit reached (" + limits.maxWorkflows() + "). "
                            + "Create an account to build more workflows.");
        }
    }

    /**
     * Checks whether the user can add another step to a specific workflow.
     * Throws 403 if the step count has reached the tier's maximum.
     *
     * @param currentStepCount the number of steps already in the workflow
     */
    public void enforceStepLimit(int currentStepCount) {
        PlatformLimits limits = currentLimits();
        if (currentStepCount >= limits.maxStepsPerWorkflow()) {
            throw new ResponseStatusException(HttpStatus.FORBIDDEN,
                    "Step limit reached (" + limits.maxStepsPerWorkflow() + " per workflow). "
                            + (currentTier().isLimited()
                            ? "Verify your email to unlock more steps."
                            : "You have reached the maximum for your plan."));
        }
    }

    /**
     * Throws 403 if webhooks are not available for the current user's tier.
     */
    public void requireWebhookAccess() {
        if (!currentLimits().canUseWebhooks()) {
            throw new ResponseStatusException(HttpStatus.FORBIDDEN,
                    "Webhooks require a verified email address");
        }
    }

    /**
     * Throws 403 if connection management is not available for the current user's tier.
     */
    public void requireConnectionAccess() {
        if (!currentLimits().canManageConnections()) {
            throw new ResponseStatusException(HttpStatus.FORBIDDEN,
                    "Managing connections requires a verified email address");
        }
    }

    /**
     * Throws 403 if export is not available for the current user's tier.
     */
    public void requireExportAccess() {
        if (!currentLimits().canExportWorkflows()) {
            throw new ResponseStatusException(HttpStatus.FORBIDDEN,
                    "Exporting workflows requires a verified email address");
        }
    }

    // -------------------------------------------------------------------------
    // INTERNAL
    // -------------------------------------------------------------------------

    /// Extracts the AppUserDetails principal from the current SecurityContext.
    /// Returns null when no authentication exists (anonymous/guest requests).
    private AppUserDetails currentPrincipal() {
        Authentication auth = SecurityContextHolder.getContext().getAuthentication();
        if (auth == null || !auth.isAuthenticated()) return null;
        Object principal = auth.getPrincipal();
        if (principal instanceof AppUserDetails details) return details;
        return null;
    }
}
