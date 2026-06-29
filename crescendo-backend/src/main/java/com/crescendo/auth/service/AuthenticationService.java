package com.crescendo.auth.service;

import com.crescendo.auth.domain_event.OAuthProviderLinkedEvent;
import com.crescendo.auth.domain_event.UserLoggedInEvent;
import com.crescendo.auth.domain_event.UserPasswordChangedEvent;
import com.crescendo.auth.domain_event.UserPasswordResetEvent;
import com.crescendo.emailservice.NotificationService;
import com.crescendo.auth.dto.AuthDto;
import com.crescendo.auth.token.email.EmailVerificationToken;
import com.crescendo.auth.token.email.EmailVerificationTokenRepository;
import com.crescendo.auth.token.password.PasswordResetToken;
import com.crescendo.auth.token.password.PasswordResetTokenRepository;
import com.crescendo.enums.AuthProvider;
import com.crescendo.enums.UserRole;
import com.crescendo.security.AppUserDetails;
import com.crescendo.security.JWTService;
import com.crescendo.security.TokenPair;
import com.crescendo.shared.domain.event.DomainEventPublisher;
import com.crescendo.user.domain_event.UserEmailVerifiedEvent;
import com.crescendo.user.domain_event.UserRegisteredEvent;
import com.crescendo.user.user_command.User_command;
import com.crescendo.user.user_command.User_commandRepository;
import com.crescendo.user.user_command.user_credential.UserCredential;
import com.crescendo.user.user_command.user_credential.UserCredentialRepository;
import com.crescendo.user.user_command.user_identity.UserIdentity;
import com.crescendo.user.user_command.user_identity.UserIdentityRepository;
import jakarta.transaction.Transactional;
import org.springframework.http.HttpStatus;
import org.springframework.security.crypto.bcrypt.BCryptPasswordEncoder;
import org.springframework.stereotype.Service;
import org.springframework.web.server.ResponseStatusException;

import java.nio.charset.StandardCharsets;
import java.security.MessageDigest;
import java.security.SecureRandom;
import java.time.Duration;
import java.time.Instant;
import java.util.ArrayList;
import java.util.Base64;
import java.util.List;
import java.util.Optional;
import java.util.UUID;

/**
 * Handles all authentication flows:
 *   - Local registration & login (email/username + password)
 *   - OAuth login with automatic account linking via email
 *   - Token refresh and logout
 *   - Password reset (forgot/reset/change)
 *   - Email verification
 */
@Service
@Transactional
public class AuthenticationService {

    private final User_commandRepository userRepo;
    private final UserCredentialRepository credentialRepo;
    private final UserIdentityRepository identityRepo;
    private final PasswordResetTokenRepository passwordResetRepo;
    private final EmailVerificationTokenRepository emailVerificationRepo;
    private final JWTService jwtService;
    private final BCryptPasswordEncoder passwordEncoder;
    private final NotificationService notificationService;
    private final DomainEventPublisher eventPublisher;
    private final SecureRandom secureRandom = new SecureRandom();

    public AuthenticationService(
            User_commandRepository userRepo,
            UserCredentialRepository credentialRepo,
            UserIdentityRepository identityRepo,
            PasswordResetTokenRepository passwordResetRepo,
            EmailVerificationTokenRepository emailVerificationRepo,
            JWTService jwtService,
            BCryptPasswordEncoder passwordEncoder,
            NotificationService notificationService,
            DomainEventPublisher eventPublisher) {
        this.userRepo = userRepo;
        this.credentialRepo = credentialRepo;
        this.identityRepo = identityRepo;
        this.passwordResetRepo = passwordResetRepo;
        this.emailVerificationRepo = emailVerificationRepo;
        this.jwtService = jwtService;
        this.passwordEncoder = passwordEncoder;
        this.notificationService = notificationService;
        this.eventPublisher = eventPublisher;
    }


    /**
     * Registers a new user with a local (email + password) credential and auto-logs them in.
     * Sends an email verification link after account creation.
     */
    public AuthDto.RegisterResponse register(AuthDto.RegisterRequest req, String userAgent, String clientIp) {
        // Reject if email already belongs to an account — prevents silent overwrite of OAuth-only accounts.
        if (userRepo.findByEmailIgnoreCase(req.email()).isPresent())
            throw new ResponseStatusException(HttpStatus.CONFLICT, "Email already registered");

        // Create the canonical user record. New local users start as USER role.
        User_command user = new User_command(UUID.randomUUID(), req.username(), req.email(), UserRole.USER);
        userRepo.save(user);

        // Hash the password with BCrypt (cost 12, configured in SecurityConfig) and persist.
        credentialRepo.save(new UserCredential(UUID.randomUUID(), user, passwordEncoder.encode(req.password())));

        // Fire off the verification email so the user can confirm ownership.
        sendVerificationEmail(user.getId());

        // Publish domain event
        eventPublisher.publish(new UserRegisteredEvent(user.getId(), user.getEmailId(), user.getUserName()));

        // Auto-login immediately after registration — no need for a separate login step.
        TokenPair tokens = issueTokens(user, userAgent, clientIp, req.deviceId(), req.deviceLabel(), false);
        return new AuthDto.RegisterResponse(
                user.getId().toString(), user.getEmailId(), user.getUserName(),
                user.getRole().name(), List.of(AuthProvider.LOCAL.name()), true,
                tokens.accessToken(), tokens.accessExpiresAt(),
                tokens.refreshToken(), tokens.refreshExpiresAt(),
                "Registration successful. Please verify your email.",
                user.getCreatedAt()
        );
    }

    // OAUTH LOGIN  (called from the OAuth2 success handler after redirect)
    /**
     * Handles login or registration via an OAuth provider.
     * Account linking: if the OAuth email already exists as a local user, the provider
     * is linked to that account instead of creating a duplicate.
     *
     * @param provider         the OAuth provider (GOOGLE / GITHUB)
     * @param providerUserId   the user ID as assigned by the provider
     * @param email            the verified email returned by the provider
     * @param suggestedUsername a display name suggested by the provider (maybe adjusted for uniqueness)
     * @param userAgent        HTTP User-Agent for session tracking
     */
    public AuthDto.LoginResponse oauthLogin(AuthProvider provider, String providerUserId,
                                            String email, String suggestedUsername, String userAgent) {
        // Step 1: exact match on (provider, providerUserId) — fastest path for returning OAuth users.
        Optional<UserIdentity> identityOpt = identityRepo.findByProviderAndProviderUserId(provider, providerUserId);

        User_command user;
        if (identityOpt.isPresent()) {
            // Returning OAuth user — identity row already exists, just get the linked account.
            user = identityOpt.get().getUser();
        } else {
            // Step 2: try to find an existing account by email to link providers together.
            // This is how "login with Google" and "login with password" merge into one account
            // as long as the email address matches.
            user = userRepo.findByEmailIgnoreCase(email).orElse(null);

            if (user == null) {
                // Step 3: brand-new user — create the canonical account first.
                user = new User_command(UUID.randomUUID(), suggestedUsername, email, UserRole.USER);
                userRepo.save(user);
            }

            // OAuth providers verify the user's email before returning it to us,
            // so we can treat the account as email-verified immediately.
            if (!user.isEmailVerified()) {
                user.setEmailVerified(true);
            }

            // Link this OAuth provider to the account (whether new or pre-existing).
            identityRepo.save(new UserIdentity(UUID.randomUUID(), user, provider, providerUserId, email));
            eventPublisher.publish(new OAuthProviderLinkedEvent(user.getId(), provider));
        }

        eventPublisher.publish(new UserLoggedInEvent(user.getId(), user.getEmailId(), provider));

        Optional<UserCredential> cred = credentialRepo.findByUser_Id(user.getId());
        TokenPair tokens = issueTokens(user, userAgent, null, null, null, false);
        return buildLoginResponse(user, cred.orElse(null), tokens);
    }

    // LOGIN HELPERS (used by controller to slot MFA check between verify & issue)
    /**
     * First half of the login flow: verify identifier + password and return the user.
     * Called by the controller so it can check MFA before deciding whether to issue tokens.
     * Throws 401 on any failure using a generic message to prevent credential enumeration.
     */
    public User_command verifyLocalCredentials(AuthDto.LoginRequest req) {
        User_command user = userRepo.findByEmailIgnoreCase(req.email())
                .orElseThrow(() -> new ResponseStatusException(HttpStatus.UNAUTHORIZED, "Invalid credentials"));
        UserCredential cred = credentialRepo.findByUser_Id(user.getId())
                .orElseThrow(() -> new ResponseStatusException(HttpStatus.UNAUTHORIZED, "No password set — please use OAuth login"));
        if (!passwordEncoder.matches(req.password(), cred.getPasswordHash()))
            throw new ResponseStatusException(HttpStatus.UNAUTHORIZED, "Invalid credentials");
        return user;
    }

    /**
     * Second half of the login flow: issue tokens for a user whose identity has already been verified.
     * Reused after MFA challenge passes, after OAuth login, and after registration auto-login.
     */
    public AuthDto.LoginResponse issueLoginResponse(User_command user, String userAgent, String clientIp, String deviceId, String deviceLabel, boolean rememberMe) {
        eventPublisher.publish(new UserLoggedInEvent(user.getId(), user.getEmailId(), AuthProvider.LOCAL));

        Optional<UserCredential> cred = credentialRepo.findByUser_Id(user.getId());
        TokenPair tokens = issueTokens(user, userAgent, clientIp, deviceId, deviceLabel, rememberMe);
        return buildLoginResponse(user, cred.orElse(null), tokens);
    }

    // TOKEN REFRESH & LOGOUT
    /**
     * Validates and rotates the refresh token, returning a new access token (and optionally a new refresh).
     * Rotation is controlled by the jwt.refresh.rotate property (default true).
     */
    public AuthDto.AccessTokenResponse refreshTokens(String rawRefreshToken, String userAgent, String clientIp) {
        TokenPair pair = jwtService.refresh(rawRefreshToken, userAgent, clientIp)
                .orElseThrow(() -> new ResponseStatusException(HttpStatus.UNAUTHORIZED, "Invalid or expired refresh token"));
        return new AuthDto.AccessTokenResponse(
                pair.accessToken(), pair.accessExpiresAt(),
                pair.refreshToken(), pair.refreshExpiresAt()
        );
    }

    /**
     * Revokes the provided refresh token — the session row is deleted from user_session.
     * Subsequent refresh attempts with the same token will be rejected.
     */
    public void logout(String rawRefreshToken) {
        jwtService.revokeRefresh(rawRefreshToken);
    }

    // PASSWORD RESET
    /**
     * Initiates a password reset flow by generating a secure one-time token and persisting its hash.
     * Always returns silently even if the email is unknown — prevents account enumeration.
     */
    public void forgotPassword(AuthDto.ForgotPasswordRequest req) {
        Optional<User_command> userOpt = userRepo.findByEmailIgnoreCase(req.email());
        if (userOpt.isEmpty()) return; // don't reveal whether the email is registered

        User_command user = userOpt.get();

        // OAuth-only users have no password to reset
        if (credentialRepo.findByUser_Id(user.getId()).isEmpty()) return;

        String plainToken = generateSecureToken();
        String tokenHash = hashToken(plainToken);
        Instant expiresAt = Instant.now().plus(Duration.ofHours(1));
        passwordResetRepo.save(new PasswordResetToken(UUID.randomUUID(), user, tokenHash, expiresAt));

        // Dev mode: print the raw token to the terminal so it can be used without a real email domain.
        notificationService.sendPasswordResetToken(user.getEmailId(), plainToken);
    }

    /**
     * Consumes a password reset token and updates the user's credential.
     * Token is single-use and expires after 1 hour.
     */
    public void resetPassword(AuthDto.ResetPasswordRequest req) {
        String tokenHash = hashToken(req.resetToken());
        PasswordResetToken token = passwordResetRepo.findByTokenHash(tokenHash)
                .orElseThrow(() -> new ResponseStatusException(HttpStatus.BAD_REQUEST, "Invalid reset token"));

        if (token.getConsumedAt() != null)
            throw new ResponseStatusException(HttpStatus.BAD_REQUEST, "Reset token has already been used");

        if (Instant.now().isAfter(token.getExpiresAt()))
            throw new ResponseStatusException(HttpStatus.BAD_REQUEST, "Reset token has expired");

        UserCredential cred = credentialRepo.findByUser_Id(token.getUser().getId())
                .orElseThrow(() -> new ResponseStatusException(HttpStatus.BAD_REQUEST, "No local credential found"));

        cred.setPasswordHash(passwordEncoder.encode(req.newPassword()));

        // Mark consumed so the same link cannot be reused.
        token.setConsumedAt(Instant.now());

        eventPublisher.publish(new UserPasswordResetEvent(token.getUser().getId()));
    }

    /**
     * Allows an authenticated user to change their password by verifying the old one first.
     */
    public void changePassword(UUID userId, AuthDto.PasswordChangeRequest req) {
        UserCredential cred = credentialRepo.findByUser_Id(userId)
                .orElseThrow(() -> new ResponseStatusException(HttpStatus.BAD_REQUEST, "No local credential — account uses OAuth only"));

        if (!passwordEncoder.matches(req.oldPassword(), cred.getPasswordHash()))
            throw new ResponseStatusException(HttpStatus.UNAUTHORIZED, "Current password is incorrect");

        cred.setPasswordHash(passwordEncoder.encode(req.newPassword()));

        eventPublisher.publish(new UserPasswordChangedEvent(userId));
    }

    // EMAIL VERIFICATION
    /**
     * Generates a 24-hour email verification link and persists the token hash.
     * Called after registration and when the user requests a resend.
     */
    public void sendVerificationEmail(UUID userId) {
        User_command user = userRepo.findById(userId)
                .orElseThrow(() -> new ResponseStatusException(HttpStatus.NOT_FOUND, "User not found"));

        String plainToken = generateSecureToken();
        String tokenHash = hashToken(plainToken);
        Instant expiresAt = Instant.now().plus(Duration.ofHours(24));
        emailVerificationRepo.save(new EmailVerificationToken(UUID.randomUUID(), user, tokenHash, expiresAt));

        // Dev mode: print the raw token to the terminal so it can be used without a real email domain.
        notificationService.sendEmailVerificationToken(user.getEmailId(), plainToken);
    }

    /**
     * Validates an email verification token and marks it consumed.
     * The user's email-verified flag should be set here once the User_command entity exposes it.
     */
    public void verifyEmail(String rawToken) {
        String tokenHash = hashToken(rawToken);
        EmailVerificationToken evToken = emailVerificationRepo.findByTokenHash(tokenHash)
                .orElseThrow(() -> new ResponseStatusException(HttpStatus.BAD_REQUEST, "Invalid verification link"));

        if (evToken.getConsumedAt() != null)
            throw new ResponseStatusException(HttpStatus.BAD_REQUEST, "Verification link has already been used");

        if (Instant.now().isAfter(evToken.getExpiresAt()))
            throw new ResponseStatusException(HttpStatus.BAD_REQUEST, "Verification link has expired");

        evToken.setConsumedAt(Instant.now());

        // Mark the user's email as verified now that the token has been consumed.
        User_command user = evToken.getUser();
        user.setEmailVerified(true);
        userRepo.save(user);

        eventPublisher.publish(new UserEmailVerifiedEvent(user.getId(), user.getEmailId()));
    }

    /// Issues a fresh TokenPair using AppUserDetails built from the user + optional credential.
    private TokenPair issueTokens(User_command user, String userAgent, String clientIp, String deviceId, String deviceLabel, boolean rememberMe) {
        Optional<UserCredential> cred = credentialRepo.findByUser_Id(user.getId());
        AppUserDetails principal = AppUserDetails.from(user, cred);
        return jwtService.issueTokenPair(user, principal, userAgent, clientIp, deviceId, deviceLabel, rememberMe);
    }

    /// Builds a LoginResponse from a user, credential (maybe null for OAuth-only), and already-issued tokens.
    private AuthDto.LoginResponse buildLoginResponse(User_command user, UserCredential cred, TokenPair tokens) {
        boolean hasLocal = cred != null;
        List<String> providers = new ArrayList<>();
        if (hasLocal) providers.add(AuthProvider.LOCAL.name());
        identityRepo.findAllByUser_Id(user.getId())
                .forEach(id -> providers.add(id.getProvider().name()));
        return new AuthDto.LoginResponse(
                user.getId().toString(), user.getEmailId(), user.getUserName(),
                user.getRole().name(), providers, hasLocal,
                tokens.accessToken(), tokens.accessExpiresAt(),
                tokens.refreshToken(), tokens.refreshExpiresAt(),
                user.getCreatedAt()
        );
    }



    /// Generates 32 cryptographically random bytes and Base64url-encodes them (no padding).
    /// Used for both password-reset tokens and email-verification tokens.
    /// The raw value is emailed to the user; only its hash is stored in the DB.
    private String generateSecureToken() {
        byte[] raw = new byte[32];
        secureRandom.nextBytes(raw);
        return Base64.getUrlEncoder().withoutPadding().encodeToString(raw);
    }

    /// SHA-256 hash of a token, returned as Base64 (consistent with JWTService refresh token hashing).
    /// This is what gets stored in the DB so raw tokens are never at rest.
    private String hashToken(String plain) {
        try {
            MessageDigest md = MessageDigest.getInstance("SHA-256");
            byte[] digest = md.digest(plain.getBytes(StandardCharsets.UTF_8));
            return Base64.getEncoder().encodeToString(digest);
        } catch (Exception e) {
            throw new IllegalStateException("Cannot hash token", e);
        }
    }
}