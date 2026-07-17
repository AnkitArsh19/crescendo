package com.crescendo.user.user_query;

import com.crescendo.security.access.AccessTier;
import com.crescendo.user.UserDto;
import com.crescendo.user.user_command.User_command;
import com.crescendo.user.user_command.User_commandRepository;
import com.crescendo.user.user_command.user_credential.UserCredentialRepository;
import com.crescendo.user.user_command.user_identity.UserIdentity;
import com.crescendo.user.user_command.user_identity.UserIdentityRepository;
import com.crescendo.user.user_command.user_session.UserSession;
import com.crescendo.user.user_command.user_session.UserSessionRepository;
import com.crescendo.security.mfa.UserMFASettingRepository;
import com.crescendo.user.user_command.webauthn.PasskeyCredential_commandRepository;
import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Service;
import org.springframework.web.server.ResponseStatusException;

import java.time.Instant;
import java.util.List;
import java.util.UUID;

/**
 * Read-side service for user account queries.
 *
 * Assembles a complete {@link UserDto.AccountResponse} by aggregating data from
 * multiple command-side repositories.  Nothing is mutated — every method is
 * effectively read-only.
 *
 * NOTE: In a full CQRS setup these reads would hit the query database exclusively.
 * For now, we read from command-side repos because the query-side projections are
 * not yet event-driven.  The controller interface remains identical either way.
 */
@Service
public class User_queryService {

    private final User_commandRepository userRepo;
    private final UserCredentialRepository credentialRepo;
    private final UserIdentityRepository identityRepo;
    private final UserSessionRepository sessionRepo;
    private final UserMFASettingRepository mfaSettingRepo;
    private final PasskeyCredential_commandRepository passkeyRepo;

    public User_queryService(User_commandRepository userRepo,
                             UserCredentialRepository credentialRepo,
                             UserIdentityRepository identityRepo,
                             UserSessionRepository sessionRepo,
                             UserMFASettingRepository mfaSettingRepo,
                             PasskeyCredential_commandRepository passkeyRepo) {
        this.userRepo = userRepo;
        this.credentialRepo = credentialRepo;
        this.identityRepo = identityRepo;
        this.sessionRepo = sessionRepo;
        this.mfaSettingRepo = mfaSettingRepo;
        this.passkeyRepo = passkeyRepo;
    }
    /**
     * Builds the complete account profile for GET /users/me.
     * @param userId           authenticated user's ID
     * @param currentSessionId the session ID from the caller's JWT ("sid" claim) —
     *                         used to mark which session row is "current" in the response
     */
    public UserDto.AccountResponse getAccount(UUID userId, UUID currentSessionId) {
        User_command user = userRepo.findById(userId)
                .orElseThrow(() -> new ResponseStatusException(HttpStatus.NOT_FOUND, "User not found"));

        boolean hasLocal = credentialRepo.findByUser_Id(userId).isPresent();

        // MFA status — enrolled means a verified secret exists, enabled is the explicit toggle.
        UserDto.MfaStatusResponse mfa = mfaSettingRepo.findByUser_Id(userId)
                .map(s -> new UserDto.MfaStatusResponse(s.isVerified(), s.isEnabled()))
                .orElse(new UserDto.MfaStatusResponse(false, false));

        // Linked OAuth providers.
        List<UserDto.LinkedAccountResponse> linkedAccounts = identityRepo.findAllByUser_Id(userId)
                .stream()
                .map(this::toLinkedAccount)
                .toList();

        // Active sessions (not revoked, not expired).
        List<UserDto.ActiveSessionResponse> sessions = sessionRepo
                .findAllActiveByUserId(userId, Instant.now())
                .stream()
                .map(s -> toActiveSession(s, currentSessionId))
                .toList();

        // Access tier and plan limits.
        AccessTier tier = AccessTier.resolve(user.getRole(), user.isEmailVerified());
        UserDto.LimitsResponse limits = switch (tier) {
            case ADMIN      -> new UserDto.LimitsResponse("ADMIN",      false, -1,  -1);
            case STANDARD   -> new UserDto.LimitsResponse("STANDARD",   false, 50,  20);
            case UNVERIFIED -> new UserDto.LimitsResponse("UNVERIFIED", true,  3,   3);
            case GUEST      -> new UserDto.LimitsResponse("GUEST",      true,  1,   1);
        };

        int passkeyCount = (int) passkeyRepo.countByUserId(userId);

        return new UserDto.AccountResponse(
                user.getId().toString(),
                user.getEmailId(),
                user.getUserName(),
                user.getRole().name(),
                hasLocal,
                passkeyCount,
                user.getPasskeyNudgeDismissCount(),
                user.isPasskeyNudgeOptedOut(),
                mfa,
                linkedAccounts,
                sessions,
                user.getCreatedAt(),
                limits
        );
    }


    // DTO MAPPERS
    private UserDto.LinkedAccountResponse toLinkedAccount(UserIdentity identity) {
        return new UserDto.LinkedAccountResponse(
                identity.getId().toString(),
                identity.getProvider().name(),
                identity.getEmail(),
                identity.getLinkedAt()
        );
    }

    private UserDto.ActiveSessionResponse toActiveSession(UserSession session, UUID currentSessionId) {
        return new UserDto.ActiveSessionResponse(
                session.getId().toString(),
                session.getUserAgent() != null ? session.getUserAgent().value() : null,
                session.getClientIp() != null ? session.getClientIp().value() : null,
                session.getDeviceLabel(),
                session.getId().equals(currentSessionId),
                session.getCreatedAt(),
                session.getLastUsedAt(),
                session.getExpiresAt()
        );
    }
}
