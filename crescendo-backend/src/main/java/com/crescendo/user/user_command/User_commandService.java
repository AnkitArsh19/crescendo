package com.crescendo.user.user_command;

import com.crescendo.connections.connections_command.Connections_commandService;
import com.crescendo.emailservice.apikey.key_command.ApiKey_commandRepository;
import com.crescendo.emailservice.audience.ContactRepository;
import com.crescendo.emailservice.broadcast.BroadcastRepository;
import com.crescendo.emailservice.domain.DomainRepository;
import com.crescendo.emailservice.emailtemplate.template_command.EmailTemplate_commandRepository;
import com.crescendo.security.crypto.CryptoShreddingService;
import com.crescendo.security.mfa.UserMFABackupCodeRepository;
import com.crescendo.security.mfa.UserMFASettingRepository;
import com.crescendo.shared.domain.event.DomainEventPublisher;
import com.crescendo.storage.FileStorageService;
import com.crescendo.storage.storage_command.UploadedFile_commandRepository;
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
import com.crescendo.user.user_command.webauthn.PasskeyCredential_commandRepository;
import com.crescendo.workflow.workflow_command.Workflow_commandService;
import jakarta.transaction.Transactional;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
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
 * - Delete account (full cascade: workflows, triggers, connections, storage files, domains, passkeys, credentials, crypto-shredding)
 */
@Service
@Transactional
public class User_commandService {

    private static final Logger log = LoggerFactory.getLogger(User_commandService.class);

    private final User_commandRepository userRepo;
    private final UserCredentialRepository credentialRepo;
    private final UserIdentityRepository identityRepo;
    private final UserSessionRepository sessionRepo;
    private final UserMFASettingRepository mfaSettingRepo;
    private final UserMFABackupCodeRepository mfaBackupRepo;
    private final Workflow_commandService workflowCommandService;
    private final Connections_commandService connectionsCommandService;
    private final UploadedFile_commandRepository uploadedFileRepo;
    private final FileStorageService fileStorageService;
    private final PasskeyCredential_commandRepository passkeyRepo;
    private final DomainRepository domainRepo;
    private final ApiKey_commandRepository apiKeyRepo;
    private final EmailTemplate_commandRepository emailTemplateRepo;
    private final ContactRepository contactRepo;
    private final BroadcastRepository broadcastRepo;
    private final CryptoShreddingService cryptoShreddingService;
    private final BCryptPasswordEncoder passwordEncoder;
    private final DomainEventPublisher eventPublisher;

    public User_commandService(User_commandRepository userRepo,
            UserCredentialRepository credentialRepo,
            UserIdentityRepository identityRepo,
            UserSessionRepository sessionRepo,
            UserMFASettingRepository mfaSettingRepo,
            UserMFABackupCodeRepository mfaBackupRepo,
            Workflow_commandService workflowCommandService,
            Connections_commandService connectionsCommandService,
            UploadedFile_commandRepository uploadedFileRepo,
            FileStorageService fileStorageService,
            PasskeyCredential_commandRepository passkeyRepo,
            DomainRepository domainRepo,
            ApiKey_commandRepository apiKeyRepo,
            EmailTemplate_commandRepository emailTemplateRepo,
            ContactRepository contactRepo,
            BroadcastRepository broadcastRepo,
            CryptoShreddingService cryptoShreddingService,
            BCryptPasswordEncoder passwordEncoder,
            DomainEventPublisher eventPublisher) {
        this.userRepo = userRepo;
        this.credentialRepo = credentialRepo;
        this.identityRepo = identityRepo;
        this.sessionRepo = sessionRepo;
        this.mfaSettingRepo = mfaSettingRepo;
        this.mfaBackupRepo = mfaBackupRepo;
        this.workflowCommandService = workflowCommandService;
        this.connectionsCommandService = connectionsCommandService;
        this.uploadedFileRepo = uploadedFileRepo;
        this.fileStorageService = fileStorageService;
        this.passkeyRepo = passkeyRepo;
        this.domainRepo = domainRepo;
        this.apiKeyRepo = apiKeyRepo;
        this.emailTemplateRepo = emailTemplateRepo;
        this.contactRepo = contactRepo;
        this.broadcastRepo = broadcastRepo;
        this.cryptoShreddingService = cryptoShreddingService;
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
     * Hard-deletes the user and all related data across both CQRS command and query stores:
     * 1. Workflows: Deactivates workflows, stops background polling triggers, clears steps/edges/projections/workflows
     * 2. Connections: Purges query projections, credentials, and command connections
     * 3. Uploaded Files: Deletes physical objects from S3/disk storage, then deletes metadata records
     * 4. Email Service: Purges API keys, templates, broadcasts, contacts, and custom domains
     * 5. WebAuthn: Purges passkey credentials
     * 6. Crypto-shredding: Destroys per-user Data Encryption Key (DEK)
     * 7. MFA: Purges backup codes and MFA settings
     * 8. Auth: Revokes and purges sessions, credentials, and identities
     * 9. User Record: Deletes user row and publishes UserAccountDeletedEvent
     */
    public void deleteAccount(UUID userId) {
        log.info("[account-deletion] Initiating comprehensive cascade deletion for user {}", userId);

        // 1. Purge workflows and stop polling triggers
        try {
            workflowCommandService.purgeAllWorkflowsForUser(userId);
        } catch (Exception e) {
            log.error("[account-deletion] Error purging workflows for user {}: {}", userId, e.getMessage(), e);
            throw e;
        }

        // 2. Purge connections and projections
        try {
            connectionsCommandService.purgeAllConnectionsForUser(userId);
        } catch (Exception e) {
            log.error("[account-deletion] Error purging connections for user {}: {}", userId, e.getMessage(), e);
            throw e;
        }

        // 3. Purge uploaded files from S3/storage and database
        try {
            uploadedFileRepo.findAllByUserId(userId).forEach(file -> {
                try {
                    fileStorageService.delete(file.getStorageKey());
                } catch (Exception ex) {
                    log.warn("[account-deletion] Failed to delete file storage key {} from storage: {}", file.getStorageKey(), ex.getMessage());
                }
            });
            uploadedFileRepo.deleteAllByUserId(userId);
        } catch (Exception e) {
            log.error("[account-deletion] Error purging files for user {}: {}", userId, e.getMessage(), e);
            throw e;
        }

        // 4. Purge email service resources
        try {
            apiKeyRepo.deleteAllByUserId(userId);
            emailTemplateRepo.deleteAllByUserId(userId);
            broadcastRepo.deleteAllByUserId(userId);
            contactRepo.deleteAllByUserId(userId);
            domainRepo.deleteAllByUser_Id(userId);
        } catch (Exception e) {
            log.error("[account-deletion] Error purging email resources for user {}: {}", userId, e.getMessage(), e);
            throw e;
        }

        // 5. Purge passkeys
        try {
            passkeyRepo.deleteAllByUserId(userId);
        } catch (Exception e) {
            log.error("[account-deletion] Error purging passkeys for user {}: {}", userId, e.getMessage(), e);
            throw e;
        }

        // 6. Cryptographic Erasure: Shred per-user Data Encryption Key (DEK)
        try {
            cryptoShreddingService.shredUserKey(userId);
        } catch (Exception e) {
            log.error("[account-deletion] Error shredding encryption key for user {}: {}", userId, e.getMessage(), e);
            throw e;
        }

        // 7. Wipe MFA backup codes first (FK → user_mfa_setting via user_id, and FK → user_command)
        mfaBackupRepo.deleteAllByUserId(userId);
        mfaSettingRepo.findByUser_Id(userId).ifPresent(mfaSettingRepo::delete);

        // 8. Wipe sessions, credential, identities
        sessionRepo.findAllActiveByUserId(userId, Instant.now())
                .forEach(s -> s.setRevokedAt(Instant.now()));
        sessionRepo.deleteAll(sessionRepo.findAll().stream()
                .filter(s -> s.getUser().getId().equals(userId))
                .toList());

        credentialRepo.findByUser_Id(userId).ifPresent(credentialRepo::delete);
        identityRepo.deleteAll(identityRepo.findAllByUser_Id(userId));

        // 9. Finally delete the user row itself and publish event
        userRepo.deleteById(userId);
        eventPublisher.publish(new UserAccountDeletedEvent(userId));
        log.info("[account-deletion] Successfully completed account deletion for user {}", userId);
    }

    // PASSKEY NUDGE

    /**
     * Records a passkey-nudge dismissal for the user.
     * <p>
     * If {@code permanent} is true, sets {@code passkeyNudgeOptedOut = true} immediately,
     * which prevents the nudge from ever appearing again regardless of count or cooldown.
     * <p>
     * Otherwise, increments the temporary dismissal counter and records the timestamp
     * so the 14-day cooldown and 2-strike maximum can be enforced on the frontend.
     *
     * @param userId    authenticated user
     * @param permanent true if the user clicked "Don't ask again", false for a temporary X-close
     */
    public void dismissPasskeyNudge(UUID userId, boolean permanent) {
        User_command user = findUser(userId);
        if (permanent) {
            user.setPasskeyNudgeOptedOut(true);
        } else {
            user.setPasskeyNudgeDismissCount(user.getPasskeyNudgeDismissCount() + 1);
            user.setPasskeyNudgeLastDismissedAt(Instant.now());
        }
    }

    // HELPERS

    private User_command findUser(UUID userId) {
        return userRepo.findById(userId)
                .orElseThrow(() -> new ResponseStatusException(HttpStatus.NOT_FOUND, "User not found"));
    }
}

