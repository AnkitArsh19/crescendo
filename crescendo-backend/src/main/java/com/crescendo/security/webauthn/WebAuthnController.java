package com.crescendo.security.webauthn;

import com.crescendo.security.AppUserDetails;
import com.crescendo.security.RefreshTokenCookieService;
import com.crescendo.security.TokenPair;
import jakarta.servlet.http.HttpServletResponse;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.http.HttpHeaders;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.web.bind.annotation.*;

import jakarta.servlet.http.HttpServletRequest;
import java.time.Duration;
import java.time.Instant;
import java.util.List;
import java.util.Map;
import java.util.UUID;

@RestController
@RequestMapping("/auth/webauthn")
public class WebAuthnController {

    private final WebAuthnRegistrationService registrationService;
    private final WebAuthnLoginService loginService;
    private final com.crescendo.user.user_command.webauthn.PasskeyCredential_commandRepository credentialRepository;
    private final RefreshTokenCookieService cookieService;
    private final PasskeyRecoveryService recoveryService;
    private final com.crescendo.user.user_command.user_credential.UserCredentialRepository userCredentialRepository;
    private final PasswordlessRegistrationService passwordlessRegistrationService;
    private final ElevatedSessionService elevatedSessionService;
    private final com.crescendo.security.JWTService jwtService;
    private final com.crescendo.user.user_command.user_credential.UserCredentialRepository userCredentialRepository2;
    // Re-auth methods for elevation
    private final com.crescendo.user.user_command.User_commandRepository userCommandRepository;
    private final org.springframework.security.crypto.bcrypt.BCryptPasswordEncoder passwordEncoder;

    @Value("${app.cookie.secure:false}")
    private boolean secureCookie;

    public WebAuthnController(WebAuthnRegistrationService registrationService,
                              WebAuthnLoginService loginService,
                              com.crescendo.user.user_command.webauthn.PasskeyCredential_commandRepository credentialRepository,
                              RefreshTokenCookieService cookieService,
                              PasskeyRecoveryService recoveryService,
                              com.crescendo.user.user_command.user_credential.UserCredentialRepository userCredentialRepository,
                              PasswordlessRegistrationService passwordlessRegistrationService,
                              ElevatedSessionService elevatedSessionService,
                              com.crescendo.security.JWTService jwtService,
                              com.crescendo.user.user_command.User_commandRepository userCommandRepository,
                              org.springframework.security.crypto.bcrypt.BCryptPasswordEncoder passwordEncoder) {
        this.registrationService = registrationService;
        this.loginService = loginService;
        this.credentialRepository = credentialRepository;
        this.cookieService = cookieService;
        this.recoveryService = recoveryService;
        this.userCredentialRepository = userCredentialRepository;
        this.passwordlessRegistrationService = passwordlessRegistrationService;
        this.elevatedSessionService = elevatedSessionService;
        this.jwtService = jwtService;
        this.userCredentialRepository2 = userCredentialRepository;
        this.userCommandRepository = userCommandRepository;
        this.passwordEncoder = passwordEncoder;
    }

    /**
     * POST /auth/webauthn/register/start
     * Begins a new passkey registration ceremony for an already-authenticated user.
     * Requires a valid elevated-session token (obtained from POST /auth/elevate/passkey-mgmt)
     * issued within the last 10 minutes — prevents a hijacked session from silently planting
     * a persistent credential.
     */
    @PostMapping("/register/start")
    public WebAuthnDTOs.RegistrationStartResponse startRegistration(
            @AuthenticationPrincipal AppUserDetails user,
            @RequestBody(required = false) WebAuthnDTOs.RegistrationStartRequest request,
            @RequestHeader(value = "X-Elevated-Token", required = false) String elevatedToken) {
        if (!elevatedSessionService.consume(elevatedToken, user.getId(), ElevatedSessionService.SCOPE_PASSKEY_MGMT)) {
            throw new org.springframework.web.server.ResponseStatusException(
                    org.springframework.http.HttpStatus.FORBIDDEN,
                    "Step-up authentication required. Call POST /auth/elevate/passkey-mgmt first.");
        }
        return registrationService.startRegistration(user, request == null ? new WebAuthnDTOs.RegistrationStartRequest() : request);
    }

    @PostMapping("/register/finish")
    public ResponseEntity<Void> finishRegistration(@AuthenticationPrincipal AppUserDetails user,
                                                   @RequestBody WebAuthnDTOs.RegistrationFinishRequest request) {
        registrationService.finishRegistration(user, request);
        return ResponseEntity.ok().build();
    }

    @PostMapping("/login/start")
    public WebAuthnDTOs.LoginStartResponse startLogin(@RequestBody WebAuthnDTOs.LoginStartRequest request) {
        return loginService.startLogin(request);
    }

    @PostMapping("/login/finish")
    public ResponseEntity<TokenPair> finishLogin(@RequestBody WebAuthnDTOs.LoginFinishRequest request,
                                                 HttpServletRequest httpRequest,
                                                 HttpServletResponse httpResponse) {
        String userAgent = httpRequest.getHeader(HttpHeaders.USER_AGENT);
        String clientIp = httpRequest.getHeader("X-Forwarded-For");
        if (clientIp == null) {
            clientIp = httpRequest.getRemoteAddr();
        } else {
            clientIp = clientIp.split(",")[0].trim();
        }
        String deviceId = httpRequest.getHeader("X-Device-Id");
        String deviceLabel = httpRequest.getHeader("X-Device-Label");

        TokenPair tokens = loginService.finishLogin(request, userAgent, clientIp, deviceId, deviceLabel);
        long ttlMillis = Duration.between(Instant.now(), tokens.refreshExpiresAt()).toMillis();
        cookieService.setRefreshToken(httpResponse, tokens.refreshToken(), Math.max(0, ttlMillis), secureCookie);
        return ResponseEntity.ok(tokens);
    }

    @PostMapping("/passwordless/start")
    public ResponseEntity<Void> startPasswordlessSignup(@RequestBody WebAuthnDTOs.PasswordlessStartRequest request) {
        passwordlessRegistrationService.initiate(request);
        return ResponseEntity.noContent().build();
    }

    @PostMapping("/passwordless/verify")
    public WebAuthnDTOs.RegistrationStartResponse verifyPasswordlessSignup(@RequestBody WebAuthnDTOs.PasswordlessVerifyRequest request) {
        return passwordlessRegistrationService.verifyAndStart(request);
    }

    @PostMapping("/passwordless/finish")
    public ResponseEntity<TokenPair> finishPasswordlessSignup(@RequestBody WebAuthnDTOs.PasswordlessFinishRequest request,
                                                                HttpServletRequest httpRequest,
                                                                HttpServletResponse httpResponse) {
        String passkeyUserAgent = httpRequest.getHeader(HttpHeaders.USER_AGENT);
        String passkeyClientIp = httpRequest.getHeader("X-Forwarded-For");
        if (passkeyClientIp == null) passkeyClientIp = httpRequest.getRemoteAddr();
        else passkeyClientIp = passkeyClientIp.split(",")[0].trim();
        String passkeyDeviceId = httpRequest.getHeader("X-Device-Id");
        String passkeyDeviceLabel = httpRequest.getHeader("X-Device-Label");
        TokenPair tokens = passwordlessRegistrationService.finish(request, passkeyUserAgent, passkeyClientIp, passkeyDeviceId, passkeyDeviceLabel);
        long ttlMillis = Duration.between(Instant.now(), tokens.refreshExpiresAt()).toMillis();
        cookieService.setRefreshToken(httpResponse, tokens.refreshToken(), Math.max(0, ttlMillis), secureCookie);
        return ResponseEntity.status(org.springframework.http.HttpStatus.CREATED).body(tokens);
    }

    @PostMapping("/recovery/magic-link")
    public ResponseEntity<Void> requestRecoveryLink(@RequestBody WebAuthnDTOs.RecoveryMagicLinkRequest request) {
        recoveryService.requestMagicLink(request.email);
        return ResponseEntity.noContent().build();
    }

    @PostMapping("/recovery/register/start")
    public WebAuthnDTOs.RegistrationStartResponse startRecoveryRegistration(
            @RequestBody WebAuthnDTOs.RecoveryRegistrationStartRequest request) {
        try {
            return registrationService.startRecoveryRegistration(
                    recoveryService.validateActiveToken(request.recoveryToken),
                    new WebAuthnDTOs.RegistrationStartRequest()
            );
        } catch (IllegalArgumentException exception) {
            throw new org.springframework.web.server.ResponseStatusException(org.springframework.http.HttpStatus.BAD_REQUEST, exception.getMessage());
        }
    }

    @PostMapping("/recovery/register/finish")
    public ResponseEntity<Void> finishRecoveryRegistration(@RequestBody WebAuthnDTOs.RegistrationFinishRequest request) {
        try {
            java.util.UUID userId = recoveryService.validateActiveToken(request.recoveryToken);
            registrationService.finishRecoveryRegistration(userId, request);
            recoveryService.consume(request.recoveryToken, userId);
            return ResponseEntity.noContent().build();
        } catch (IllegalArgumentException exception) {
            throw new org.springframework.web.server.ResponseStatusException(org.springframework.http.HttpStatus.BAD_REQUEST, exception.getMessage());
        }
    }

    @GetMapping("/credentials")
    public List<WebAuthnDTOs.CredentialResponse> listCredentials(@AuthenticationPrincipal AppUserDetails user) {
        return credentialRepository.findByUserId(user.getId()).stream().map(credential ->
                new WebAuthnDTOs.CredentialResponse(
                        credential.getId().toString(),
                        credential.getCredentialName(),
                        credential.getTransports() == null ? new String[0] : credential.getTransports().split(","),
                        credential.isBackupEligible(),
                        credential.isBackedUp(),
                        credential.getCreatedAt(),
                        credential.getLastUsedAt()
                )
        ).toList();
    }
    
    /**
     * DELETE /auth/webauthn/credentials/{id}
     * Revokes a registered passkey. Requires an elevated-session token.
     * Blocks removal if it would leave the user with no login method.
     */
    @DeleteMapping("/credentials/{id}")
    public ResponseEntity<Void> deleteCredential(
            @AuthenticationPrincipal AppUserDetails user,
            @PathVariable UUID id,
            @RequestHeader(value = "X-Elevated-Token", required = false) String elevatedToken) {
        if (!elevatedSessionService.consume(elevatedToken, user.getId(), ElevatedSessionService.SCOPE_PASSKEY_MGMT)) {
            throw new org.springframework.web.server.ResponseStatusException(
                    org.springframework.http.HttpStatus.FORBIDDEN,
                    "Step-up authentication required. Call POST /auth/elevate/passkey-mgmt first.");
        }
        if (userCredentialRepository.findByUser_Id(user.getId()).isEmpty()
                && credentialRepository.countByUserId(user.getId()) <= 1) {
            throw new org.springframework.web.server.ResponseStatusException(
                    org.springframework.http.HttpStatus.CONFLICT,
                    "Add another passkey or a password before removing your final passkey");
        }
        return credentialRepository.findById(id)
                .filter(cred -> cred.getUser().getId().equals(user.getId()))
                .map(cred -> {
                    credentialRepository.delete(cred);
                    return ResponseEntity.ok().<Void>build();
                })
                .orElse(ResponseEntity.notFound().build());
    }

    /**
     * POST /auth/elevate/passkey-mgmt
     * Step-up authentication endpoint. Verifies the user's password (or an existing passkey
     * ceremony could be added later) and issues a 10-minute scoped elevated-session token.
     * The token must be passed as X-Elevated-Token on subsequent passkey add/delete requests.
     *
     * Only accepts password re-authentication for now. TOTP re-auth can be layered on top
     * later but must be added to this endpoint specifically — not as a general step-up.
     */
    @PostMapping("/elevate/passkey-mgmt")
    public ResponseEntity<Map<String, String>> elevateForPasskeyMgmt(
            @AuthenticationPrincipal AppUserDetails principal,
            @RequestBody Map<String, String> body) {
        String password = body.get("password");
        if (password == null || password.isBlank()) {
            throw new org.springframework.web.server.ResponseStatusException(
                    org.springframework.http.HttpStatus.BAD_REQUEST, "password is required");
        }
        // Load the credential and verify the password.
        var credential = userCredentialRepository2.findByUser_Id(principal.getId())
                .orElseThrow(() -> new org.springframework.web.server.ResponseStatusException(
                        org.springframework.http.HttpStatus.BAD_REQUEST,
                        "This account has no password. Use an existing passkey to authenticate."));
        if (!passwordEncoder.matches(password, credential.getPasswordHash())) {
            throw new org.springframework.web.server.ResponseStatusException(
                    org.springframework.http.HttpStatus.UNAUTHORIZED, "Incorrect password");
        }
        String token = elevatedSessionService.issue(principal.getId(), ElevatedSessionService.SCOPE_PASSKEY_MGMT);
        return ResponseEntity.ok(Map.of(
                "elevatedToken", token,
                "scope", ElevatedSessionService.SCOPE_PASSKEY_MGMT,
                "expiresInSeconds", "600"
        ));
    }
}
