package com.crescendo.security.webauthn;

import com.crescendo.security.AppUserDetails;
import com.crescendo.emailservice.NotificationService;
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
import com.webauthn4j.data.client.challenge.Challenge;
import com.webauthn4j.data.client.challenge.DefaultChallenge;
import com.webauthn4j.server.ServerProperty;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.server.ResponseStatusException;

import java.util.Base64;
import java.util.List;
import java.util.UUID;

@Service
public class WebAuthnRegistrationService {

    private final WebAuthnManager webAuthnManager;
    private final WebAuthnChallengeService challengeService;
    private final PasskeyCredential_commandRepository credentialRepository;
    private final User_commandRepository userRepository;
    private final NotificationService notificationService;

    @Value("${app.webauthn.rp-id}")
    private String rpId;

    @Value("${app.webauthn.rp-name}")
    private String rpName;

    @Value("${app.frontend-url:https://app.crescendo.run}")
    private String frontendUrl;

    public WebAuthnRegistrationService(WebAuthnManager webAuthnManager,
                                       WebAuthnChallengeService challengeService,
                                       PasskeyCredential_commandRepository credentialRepository,
                                       User_commandRepository userRepository,
                                       NotificationService notificationService) {
        this.webAuthnManager = webAuthnManager;
        this.challengeService = challengeService;
        this.credentialRepository = credentialRepository;
        this.userRepository = userRepository;
        this.notificationService = notificationService;
    }

    public WebAuthnDTOs.RegistrationStartResponse startRegistration(AppUserDetails user,
                                                                     WebAuthnDTOs.RegistrationStartRequest request) {
        return startRegistration(user.getId(), user.getUsername(), request);
    }

    public WebAuthnDTOs.RegistrationStartResponse startRecoveryRegistration(UUID userId,
                                                                              WebAuthnDTOs.RegistrationStartRequest request) {
        User_command user = userRepository.findById(userId)
                .orElseThrow(() -> new ResponseStatusException(HttpStatus.NOT_FOUND, "User not found"));
        return startRegistration(userId, user.getUserName(), request);
    }

    private WebAuthnDTOs.RegistrationStartResponse startRegistration(UUID userId, String username,
                                                                       WebAuthnDTOs.RegistrationStartRequest request) {
        WebAuthnChallengeService.ChallengeTransaction transaction = challengeService.createChallenge(userId);

        WebAuthnDTOs.RegistrationStartResponse response = new WebAuthnDTOs.RegistrationStartResponse();
        response.rp = new WebAuthnDTOs.RegistrationStartResponse.Rp();
        response.rp.id = rpId;
        response.rp.name = rpName;

        response.user = new WebAuthnDTOs.RegistrationStartResponse.User();
        response.user.id = Base64.getUrlEncoder().withoutPadding().encodeToString(userId.toString().getBytes());
        response.user.name = username;
        response.user.displayName = username;

        response.challenge = transaction.challenge();
        response.transactionId = transaction.transactionId();
        
        // ES256 and RS256
        response.pubKeyCredParams = List.of(
            new WebAuthnDTOs.RegistrationStartResponse.PubKeyCredParam(-7),
            new WebAuthnDTOs.RegistrationStartResponse.PubKeyCredParam(-257)
        );

        response.authenticatorSelection = new WebAuthnDTOs.RegistrationStartResponse.AuthenticatorSelection();
        response.excludeCredentials = credentialRepository.findByUserId(userId).stream().map(credential -> {
            WebAuthnDTOs.RegistrationStartResponse.ExcludeCredential excluded = new WebAuthnDTOs.RegistrationStartResponse.ExcludeCredential();
            excluded.id = Base64.getUrlEncoder().withoutPadding().encodeToString(credential.getCredentialId());
            excluded.transports = credential.getTransports() == null ? null : credential.getTransports().split(",");
            return excluded;
        }).toList();
        response.timeout = 60000;
        // Direct attestation leaks device-identifying information and is unnecessary
        // for a consumer passkey service.
        response.attestation = "none";

        return response;
    }

    @Transactional
    public void finishRegistration(AppUserDetails userDetails, WebAuthnDTOs.RegistrationFinishRequest request) {
        finishRegistration(userDetails.getId(), request);
    }

    @Transactional
    public void finishRecoveryRegistration(UUID userId, WebAuthnDTOs.RegistrationFinishRequest request) {
        finishRegistration(userId, request);
    }

    private void finishRegistration(UUID userId, WebAuthnDTOs.RegistrationFinishRequest request) {
        WebAuthnChallengeService.ChallengeContext challengeContext = challengeService.consumeChallenge(request.transactionId);
        if (challengeContext == null || !userId.equals(challengeContext.expectedUserId())) {
            throw new ResponseStatusException(HttpStatus.BAD_REQUEST, "Challenge expired or invalid");
        }

        byte[] clientDataJSON = Base64.getUrlDecoder().decode(request.response.clientDataJSON);
        byte[] attestationObject = Base64.getUrlDecoder().decode(request.response.attestationObject);
        String clientExtensionsJSON = null;

        Origin origin = new Origin(frontendUrl);
        Challenge challenge = new DefaultChallenge(challengeContext.challenge());
        ServerProperty serverProperty = new ServerProperty(origin, rpId, challenge, null);
        
        RegistrationRequest registrationRequest = new RegistrationRequest(
                attestationObject,
                clientDataJSON,
                clientExtensionsJSON
        );

        RegistrationParameters registrationParameters = new RegistrationParameters(
                serverProperty,
                true,  // a passkey must perform local user verification
                true   // user presence required
        );

        RegistrationData registrationData = webAuthnManager.parse(registrationRequest);
        webAuthnManager.verify(registrationData, registrationParameters);

        // Success! Save to DB.
        User_command user = userRepository.findById(userId)
                .orElseThrow(() -> new ResponseStatusException(HttpStatus.NOT_FOUND, "User not found"));

        byte[] credentialId = registrationData.getAttestationObject().getAuthenticatorData().getAttestedCredentialData().getCredentialId();
        byte[] publicKey = registrationData.getAttestationObject().getAuthenticatorData().getAttestedCredentialData().getCOSEKey().getPublicKey().getEncoded();
        byte[] attestedCredentialData = new AttestedCredentialDataConverter(new ObjectConverter()).convert(
                registrationData.getAttestationObject().getAuthenticatorData().getAttestedCredentialData());
        long signCount = registrationData.getAttestationObject().getAuthenticatorData().getSignCount();
        
        String transports = null;
        if (request.response.transports != null) {
            transports = String.join(",", request.response.transports);
        }
        
        byte flags = registrationData.getAttestationObject().getAuthenticatorData().getFlags();
        boolean backupEligible = (flags & 0x08) != 0;
        boolean isBackedUp = (flags & 0x10) != 0;
        boolean userVerificationInitialized = (flags & 0x04) != 0;

        if (credentialRepository.findByCredentialId(credentialId).isPresent()) {
            throw new ResponseStatusException(HttpStatus.CONFLICT, "This passkey is already registered");
        }

        PasskeyCredential_command passkey = new PasskeyCredential_command(
                UUID.randomUUID(),
                user,
                credentialId,
                publicKey,
                attestedCredentialData,
                signCount,
                transports,
                backupEligible,
                isBackedUp,
                userVerificationInitialized,
                request.credentialName == null || request.credentialName.isBlank() ? "Passkey" : request.credentialName.trim()
        );

        credentialRepository.save(passkey);
        notificationService.sendPasskeyAddedEmail(user.getEmailId(), passkey.getCredentialName());
    }
}
