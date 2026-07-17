package com.crescendo.security.webauthn;

import com.crescendo.emailservice.NotificationService;
import com.crescendo.enums.UserRole;
import com.crescendo.security.AppUserDetails;
import com.crescendo.security.JWTService;
import com.crescendo.security.TokenPair;
import com.crescendo.user.user_command.User_command;
import com.crescendo.user.user_command.User_commandRepository;
import com.crescendo.user.user_command.webauthn.PasskeyCredential_command;
import com.crescendo.user.user_command.webauthn.PasskeyCredential_commandRepository;
import com.webauthn4j.WebAuthnManager;
import com.webauthn4j.converter.AttestedCredentialDataConverter;
import com.webauthn4j.converter.util.ObjectConverter;
import com.webauthn4j.data.RegistrationData;
import com.webauthn4j.data.RegistrationParameters;
import com.webauthn4j.data.RegistrationRequest;
import com.webauthn4j.data.client.Origin;
import com.webauthn4j.data.client.challenge.DefaultChallenge;
import com.webauthn4j.server.ServerProperty;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.data.redis.core.StringRedisTemplate;
import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.server.ResponseStatusException;

import java.nio.charset.StandardCharsets;
import java.security.MessageDigest;
import java.security.SecureRandom;
import java.time.Duration;
import java.util.Base64;
import java.util.Optional;
import java.util.UUID;

/** Passwordless sign-up: verify email first, then create the account only after WebAuthn succeeds. */
@Service
public class PasswordlessRegistrationService {
    private static final Duration TTL = Duration.ofMinutes(10);
    private static final String OTP_PREFIX = "passwordless:otp:";
    private static final String VERIFIED_PREFIX = "passwordless:verified:";

    private final StringRedisTemplate redisTemplate;
    private final NotificationService notificationService;
    private final User_commandRepository userRepository;
    private final PasskeyCredential_commandRepository passkeyRepository;
    private final WebAuthnChallengeService challengeService;
    private final WebAuthnManager webAuthnManager;
    private final JWTService jwtService;
    private final SecureRandom secureRandom = new SecureRandom();

    @Value("${app.webauthn.rp-id}") private String rpId;
    @Value("${app.webauthn.rp-name}") private String rpName;
    @Value("${app.frontend-url:https://app.crescendo.run}") private String frontendUrl;

    public PasswordlessRegistrationService(StringRedisTemplate redisTemplate, NotificationService notificationService,
                                           User_commandRepository userRepository, PasskeyCredential_commandRepository passkeyRepository,
                                           WebAuthnChallengeService challengeService, WebAuthnManager webAuthnManager,
                                           JWTService jwtService) {
        this.redisTemplate = redisTemplate; this.notificationService = notificationService;
        this.userRepository = userRepository; this.passkeyRepository = passkeyRepository;
        this.challengeService = challengeService; this.webAuthnManager = webAuthnManager; this.jwtService = jwtService;
    }

    public void initiate(WebAuthnDTOs.PasswordlessStartRequest request) {
        String email = normalizedEmail(request.email);
        String username = normalizedUsername(request.username);
        if (userRepository.findByEmailIgnoreCase(email).isPresent()) {
            // Send a distinct email to the real inbox owner explaining they already have an
            // account and can add a passkey from Settings. The HTTP response to the caller
            // remains identical to the success case — this is the only path that leaks no
            // information about whether the address is registered (anti-enumeration preserved).
            notificationService.sendAccountExistsEmail(email);
            return;
        }
        String otp = "%06d".formatted(secureRandom.nextInt(1_000_000));
        redisTemplate.opsForValue().set(OTP_PREFIX + emailKey(email), hash(otp) + "|" + username + "|0", TTL);
        notificationService.sendPasswordlessSignupOtp(email, otp);
    }


    public WebAuthnDTOs.RegistrationStartResponse verifyAndStart(WebAuthnDTOs.PasswordlessVerifyRequest request) {
        String email = normalizedEmail(request.email);
        String key = OTP_PREFIX + emailKey(email);
        String value = redisTemplate.opsForValue().get(key);
        if (value == null) throw new ResponseStatusException(HttpStatus.BAD_REQUEST, "Code expired or invalid");
        String[] parts = value.split("\\|", 3);
        int attempts = Integer.parseInt(parts[2]);
        if (!constantTimeEquals(parts[0], hash(request.otp)) || attempts >= 3) {
            if (attempts >= 2) redisTemplate.delete(key);
            else redisTemplate.opsForValue().set(key, parts[0] + "|" + parts[1] + "|" + (attempts + 1), TTL);
            throw new ResponseStatusException(HttpStatus.BAD_REQUEST, "Code expired or invalid");
        }
        redisTemplate.delete(key);

        UUID pendingUserId = UUID.randomUUID();
        String verificationSessionId = randomToken();
        redisTemplate.opsForValue().set(VERIFIED_PREFIX + verificationSessionId,
                email + "|" + parts[1] + "|" + pendingUserId, TTL);
        WebAuthnChallengeService.ChallengeTransaction transaction = challengeService.createChallenge(pendingUserId);
        return registrationOptions(pendingUserId, parts[1], transaction, verificationSessionId);
    }

    @Transactional
    public TokenPair finish(WebAuthnDTOs.PasswordlessFinishRequest request, String userAgent, String clientIp, String deviceId, String deviceLabel) {
        String session = redisTemplate.opsForValue().get(VERIFIED_PREFIX + request.verificationSessionId);
        if (session == null) throw new ResponseStatusException(HttpStatus.BAD_REQUEST, "Verified signup session expired");
        String[] parts = session.split("\\|", 3);
        String email = parts[0]; String username = parts[1]; UUID userId = UUID.fromString(parts[2]);
        WebAuthnChallengeService.ChallengeContext challenge = challengeService.consumeChallenge(request.transactionId);
        if (challenge == null || !userId.equals(challenge.expectedUserId())) {
            throw new ResponseStatusException(HttpStatus.BAD_REQUEST, "Challenge expired or invalid");
        }
        if (userRepository.findByEmailIgnoreCase(email).isPresent()) {
            throw new ResponseStatusException(HttpStatus.CONFLICT, "Unable to create account");
        }

        RegistrationData registration = verifyResponse(request, challenge.challenge());
        User_command user = new User_command(userId, username, email, UserRole.USER);
        user.setEmailVerified(true);
        userRepository.save(user);
        passkeyRepository.save(toCredential(user, registration, request));
        redisTemplate.delete(VERIFIED_PREFIX + request.verificationSessionId);
        AppUserDetails details = AppUserDetails.from(user, Optional.empty());
        return jwtService.issueTokenPair(user, details, userAgent, clientIp, deviceId, deviceLabel, false);
    }

    private RegistrationData verifyResponse(WebAuthnDTOs.RegistrationFinishRequest request, String challenge) {
        byte[] clientData = Base64.getUrlDecoder().decode(request.response.clientDataJSON);
        byte[] attestation = Base64.getUrlDecoder().decode(request.response.attestationObject);
        RegistrationRequest registrationRequest = new RegistrationRequest(attestation, clientData, (String) null);
        RegistrationData data = webAuthnManager.parse(registrationRequest);
        webAuthnManager.verify(data, new RegistrationParameters(
                new ServerProperty(new Origin(frontendUrl), rpId, new DefaultChallenge(challenge), null), true, true));
        return data;
    }

    private PasskeyCredential_command toCredential(User_command user, RegistrationData data, WebAuthnDTOs.RegistrationFinishRequest request) {
        var authenticatorData = data.getAttestationObject().getAuthenticatorData();
        byte[] credentialId = authenticatorData.getAttestedCredentialData().getCredentialId();
        if (passkeyRepository.findByCredentialId(credentialId).isPresent()) throw new ResponseStatusException(HttpStatus.CONFLICT, "Passkey already registered");
        byte flags = authenticatorData.getFlags();
        String transports = request.response.transports == null ? null : String.join(",", request.response.transports);
        return new PasskeyCredential_command(UUID.randomUUID(), user, credentialId,
                authenticatorData.getAttestedCredentialData().getCOSEKey().getPublicKey().getEncoded(),
                new AttestedCredentialDataConverter(new ObjectConverter()).convert(authenticatorData.getAttestedCredentialData()),
                authenticatorData.getSignCount(), transports, (flags & 0x08) != 0, (flags & 0x10) != 0,
                (flags & 0x04) != 0, request.credentialName == null || request.credentialName.isBlank() ? "Passkey" : request.credentialName.trim());
    }

    private WebAuthnDTOs.RegistrationStartResponse registrationOptions(UUID userId, String username,
                                                                         WebAuthnChallengeService.ChallengeTransaction transaction,
                                                                         String verificationSessionId) {
        WebAuthnDTOs.RegistrationStartResponse response = new WebAuthnDTOs.RegistrationStartResponse();
        response.rp = new WebAuthnDTOs.RegistrationStartResponse.Rp(); response.rp.id = rpId; response.rp.name = rpName;
        response.user = new WebAuthnDTOs.RegistrationStartResponse.User();
        response.user.id = Base64.getUrlEncoder().withoutPadding().encodeToString(userId.toString().getBytes(StandardCharsets.UTF_8));
        response.user.name = username; response.user.displayName = username;
        response.challenge = transaction.challenge(); response.transactionId = transaction.transactionId();
        response.pubKeyCredParams = java.util.List.of(new WebAuthnDTOs.RegistrationStartResponse.PubKeyCredParam(-7), new WebAuthnDTOs.RegistrationStartResponse.PubKeyCredParam(-257));
        response.authenticatorSelection = new WebAuthnDTOs.RegistrationStartResponse.AuthenticatorSelection();
        response.timeout = 60000; response.attestation = "none";
        response.verificationSessionId = verificationSessionId;
        return response;
    }

    private String normalizedEmail(String email) { if (email == null || !email.matches("^[^\\s@]+@[^\\s@]+\\.[^\\s@]+$")) throw new ResponseStatusException(HttpStatus.BAD_REQUEST, "Enter a valid email address"); return email.trim().toLowerCase(); }
    private String normalizedUsername(String value) { if (value == null || value.isBlank() || value.trim().length() > 100) throw new ResponseStatusException(HttpStatus.BAD_REQUEST, "Enter a username"); return value.trim(); }
    private String emailKey(String email) { return hash(email); }
    private String hash(String value) { try { return Base64.getUrlEncoder().withoutPadding().encodeToString(MessageDigest.getInstance("SHA-256").digest(value.getBytes(StandardCharsets.UTF_8))); } catch (Exception e) { throw new IllegalStateException(e); } }
    private boolean constantTimeEquals(String a, String b) { return MessageDigest.isEqual(a.getBytes(StandardCharsets.UTF_8), b.getBytes(StandardCharsets.UTF_8)); }
    private String randomToken() { byte[] bytes = new byte[32]; secureRandom.nextBytes(bytes); return Base64.getUrlEncoder().withoutPadding().encodeToString(bytes); }
}
