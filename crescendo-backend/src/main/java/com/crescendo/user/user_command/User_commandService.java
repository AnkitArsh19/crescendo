package com.crescendo.user.user_command;

import com.crescendo.shared.domain.event.DomainEventPublisher;
import com.crescendo.user.UserDto;
import com.crescendo.user.domain_event.OAuthProviderUnlinkedEvent;
import com.crescendo.user.domain_event.UserAccountDeletedEvent;
import com.crescendo.user.domain_event.UserProfileUpdatedEvent;
import com.crescendo.user.domain_event.UserSessionRevokedEvent;
import com.crescendo.user.user_command.user_credential.UserCredential;
import com.crescendo.user.user_command.user_credential.UserCredentialRepository;
import com.crescendo.user.user_command.user_identity.UserIdentity;
import com.crescendo.user.user_command.user_identity.UserIdentityRepository;
import com.crescendo.user.user_command.user_session.UserSession;
import com.crescendo.user.user_command.user_session.UserSessionRepository;
import com.crescendo.security.mfa.UserMFABackupCodeRepository;
import com.crescendo.security.mfa.UserMFASettingRepository;
import jakarta.transaction.Transactional;
import org.springframework.http.HttpStatus;
import org.springframework.security.crypto.bcrypt.BCryptPasswordEncoder;
import org.springframework.stereotype.Service;
import org.springframework.web.server.ResponseStatusException;

import java.time.Instant;
import java.util.List;
import java.util.UUID;

/**
 * Write-side service for user account management.
 *
 * Every method mutates state in the command database and is wrapped in
 * a transaction so partial updates never leak.
 *
 * Responsibilities:
 * - Update profile (username)
 * - Set password (for OAuth-only users adding local login)
 * - Unlink an OAuth provider (Google / GitHub)
 * - Revoke individual or all sessions
 * - Delete account (cascade: credential, identities, MFA, sessions)
 */
@Service
@Transactional
public class User_commandService {

    private final User_commandRepository userRepo;
    private final UserCredentialRepository credentialRepo;
    private final UserIdentityRepository identityRepo;
    private final UserSessionRepository sessionRepo;
    private final UserMFASettingRepository mfaSettingRepo;
    private final UserMFABackupCodeRepository mfaBackupRepo;
    private final BCryptPasswordEncoder passwordEncoder;
    private final DomainEventPublisher eventPublisher;

    public User_commandService(User_commandRepository userRepo,
            UserCredentialRepository credentialRepo,
            UserIdentityRepository identityRepo,
            UserSessionRepository sessionRepo,
            UserMFASettingRepository mfaSettingRepo,
            UserMFABackupCodeRepository mfaBackupRepo,
            BCryptPasswordEncoder passwordEncoder,
            DomainEventPublisher eventPublisher) {
        this.userRepo = userRepo;
        this.credentialRepo = credentialRepo;
        this.identityRepo = identityRepo;
        this.sessionRepo = sessionRepo;
        this.mfaSettingRepo = mfaSettingRepo;
        this.mfaBackupRepo = mfaBackupRepo;
        this.passwordEncoder = passwordEncoder;
        this.eventPublisher = eventPublisher;
    }

    // PROFILE

    /**
     * Updates the display username.
     */
    public void updateUsername(UUID userId, UserDto.UpdateProfileRequest req) {
        User_command user = findUser(userId);
        user.setUserName(req.username());
        eventPublisher.publish(new UserProfileUpdatedEvent(userId, req.username()));
    }

    // PASSWORD

    /**
     * Sets a brand-new password for an OAuth-only account.
     * After this, the user has both OAuth and LOCAL login methods available.
     * Throws 409 if a local credential already exists (use /auth/change-password
     * instead).
     */
    public void setPassword(UUID userId, UserDto.SetPasswordRequest req) {
        User_command user = findUser(userId);
        if (credentialRepo.findByUser_Id(userId).isPresent())
            throw new ResponseStatusException(HttpStatus.CONFLICT,
                    "Password already set — use PATCH /auth/change-password to change it");

        credentialRepo.save(new UserCredential(UUID.randomUUID(), user, passwordEncoder.encode(req.password())));
    }

    // LINKED PROVIDERS

    /**
     * Unlinks an OAuth provider from the account.
     *
     * Safety rule: the user must have at least one remaining login method after
     * unlinking.
     * If they unlink their only OAuth provider with no local credential, they're
     * locked out.
     * So we forbid the operation in that case.
     */
    public void unlinkProvider(UUID userId, UserDto.UnlinkProviderRequest req) {
        List<UserIdentity> identities = identityRepo.findAllByUser_Id(userId);
        boolean hasLocal = credentialRepo.findByUser_Id(userId).isPresent();

        // Find the identity row for the requested provider.
        UserIdentity target = identities.stream()
                .filter(id -> id.getProvider() == req.provider())
                .findFirst()
                .orElseThrow(() -> new ResponseStatusException(HttpStatus.NOT_FOUND,
                        req.provider() + " is not linked to this account"));

        // Guard: ensure user has another way to log in after unlinking.
        int remainingProviders = identities.size() - 1; // excluding the one being unlinked
        if (!hasLocal && remainingProviders == 0)
            throw new ResponseStatusException(HttpStatus.BAD_REQUEST,
                    "Cannot unlink last login method — set a password first or link another provider");

        identityRepo.delete(target);
        eventPublisher.publish(new OAuthProviderUnlinkedEvent(userId, req.provider()));
    }

    // SESSIONS

    /**
     * Revokes a single session (logs out a specific device).
     * The caller can revoke any of their own sessions; the session ID is a UUID
     * visible in the UI.
     */
    public void revokeSession(UUID userId, UUID sessionId) {
        UserSession session = sessionRepo.findById(sessionId)
                .orElseThrow(() -> new ResponseStatusException(HttpStatus.NOT_FOUND, "Session not found"));

        // Ownership check: only the session's owner can revoke it.
        if (!session.getUser().getId().equals(userId))
            throw new ResponseStatusException(HttpStatus.FORBIDDEN, "Not your session");

        session.setRevokedAt(Instant.now());
        eventPublisher.publish(new UserSessionRevokedEvent(userId, false));
    }

    /**
     * Revokes all active sessions except the current one.
     * This is "Log out everywhere else" — keeps the caller logged in.
     *
     * @param currentSessionId the caller's own session (extracted from their JWT).
     */
    public void revokeAllOtherSessions(UUID userId, UUID currentSessionId) {
        List<UserSession> active = sessionRepo.findAllActiveByUserId(userId, Instant.now());
        Instant now = Instant.now();
        for (UserSession s : active) {
            if (!s.getId().equals(currentSessionId)) {
                s.setRevokedAt(now);
            }
        }
        eventPublisher.publish(new UserSessionRevokedEvent(userId, true));
    }

    // ACCOUNT DELETION

    /**
     * Hard-deletes the user and all related data:
     * UserMFABackupCode → UserMFASetting → UserSession → UserCredential →
     * UserIdentity → User_command
     *
     * In production, you'd typically soft-delete + schedule purge, but for an MVP
     * this is fine.
     * Order matters because of foreign key constraints.
     */
    public void deleteAccount(UUID userId) {
        // Wipe MFA backup codes first (FK → user_mfa_setting via user_id, and FK →
        // user_command)
        mfaBackupRepo.deleteAllByUserId(userId);
        mfaSettingRepo.findByUser_Id(userId).ifPresent(mfaSettingRepo::delete);

        // Wipe sessions, credential, identities
        sessionRepo.findAllActiveByUserId(userId, Instant.now())
                .forEach(s -> s.setRevokedAt(Instant.now()));
        // Delete all sessions (active + revoked + expired)
        sessionRepo.deleteAll(sessionRepo.findAll().stream()
                .filter(s -> s.getUser().getId().equals(userId))
                .toList());

        credentialRepo.findByUser_Id(userId).ifPresent(credentialRepo::delete);
        identityRepo.deleteAll(identityRepo.findAllByUser_Id(userId));

        // Finally delete the user row itself.
        userRepo.deleteById(userId);
        eventPublisher.publish(new UserAccountDeletedEvent(userId));
    }

    // HELPERS

    private User_command findUser(UUID userId) {
        return userRepo.findById(userId)
                .orElseThrow(() -> new ResponseStatusException(HttpStatus.NOT_FOUND, "User not found"));
    }
}
