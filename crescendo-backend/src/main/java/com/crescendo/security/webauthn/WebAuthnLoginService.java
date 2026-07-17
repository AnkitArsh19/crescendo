package com.crescendo.security.webauthn;

import com.crescendo.security.AppUserDetails;
import com.crescendo.security.AppUserDetailsService;
import com.crescendo.security.JWTService;
import com.crescendo.user.user_command.User_command;
import com.crescendo.user.user_command.User_commandRepository;
import com.crescendo.user.user_command.webauthn.PasskeyCredential_command;
import com.crescendo.user.user_command.webauthn.PasskeyCredential_commandRepository;
import com.webauthn4j.WebAuthnManager;
import com.webauthn4j.converter.AttestedCredentialDataConverter;
import com.webauthn4j.converter.util.ObjectConverter;
import com.webauthn4j.credential.CredentialRecord;
import com.webauthn4j.credential.CredentialRecordImpl;
import com.webauthn4j.data.AuthenticationData;
import com.webauthn4j.data.AuthenticationParameters;
import com.webauthn4j.data.AuthenticationRequest;
import com.webauthn4j.data.client.Origin;
import com.webauthn4j.data.client.challenge.Challenge;
import com.webauthn4j.data.client.challenge.DefaultChallenge;
import com.webauthn4j.server.ServerProperty;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.server.ResponseStatusException;

import java.time.Instant;
import java.util.Base64;
import java.util.List;
import java.util.stream.Collectors;

@Service
public class WebAuthnLoginService {

    private final WebAuthnManager webAuthnManager;
    private final WebAuthnChallengeService challengeService;
    private final PasskeyCredential_commandRepository credentialRepository;
    private final User_commandRepository userRepository;
    private final AppUserDetailsService userDetailsService;
    private final JWTService jwtService;

    @Value("${app.webauthn.rp-id}")
    private String rpId;

    @Value("${app.frontend-url:https://app.crescendo.run}")
    private String frontendUrl;

    public WebAuthnLoginService(WebAuthnManager webAuthnManager,
                                WebAuthnChallengeService challengeService,
                                PasskeyCredential_commandRepository credentialRepository,
                                User_commandRepository userRepository,
                                AppUserDetailsService userDetailsService,
                                JWTService jwtService) {
        this.webAuthnManager = webAuthnManager;
        this.challengeService = challengeService;
        this.credentialRepository = credentialRepository;
        this.userRepository = userRepository;
        this.userDetailsService = userDetailsService;
        this.jwtService = jwtService;
    }

    public WebAuthnDTOs.LoginStartResponse startLogin(WebAuthnDTOs.LoginStartRequest request) {
        User_command user = null;
        if (request.email != null && !request.email.isBlank()) {
            // Do not reveal whether the account exists. An empty allowCredentials
            // list gives the browser the same result as an account without passkeys.
            user = userRepository.findByEmailIgnoreCase(request.email).orElse(null);
        }
        WebAuthnChallengeService.ChallengeTransaction transaction = challengeService.createChallenge(
                user == null ? null : user.getId());

        WebAuthnDTOs.LoginStartResponse response = new WebAuthnDTOs.LoginStartResponse();
        response.challenge = transaction.challenge();
        response.transactionId = transaction.transactionId();
        response.rpId = rpId;
        response.timeout = 60000;

        if (user != null) {
            List<PasskeyCredential_command> credentials = credentialRepository.findByUserId(user.getId());
            response.allowCredentials = credentials.stream().map(c -> {
                WebAuthnDTOs.LoginStartResponse.AllowCredential ac = new WebAuthnDTOs.LoginStartResponse.AllowCredential();
                ac.id = Base64.getUrlEncoder().withoutPadding().encodeToString(c.getCredentialId());
                if (c.getTransports() != null) {
                    ac.transports = c.getTransports().split(",");
                }
                return ac;
            }).collect(Collectors.toList());
        }

        return response;
    }

    @Transactional
    public com.crescendo.security.TokenPair finishLogin(WebAuthnDTOs.LoginFinishRequest request, String userAgent, String clientIp, String deviceId, String deviceLabel) {
        WebAuthnChallengeService.ChallengeContext challengeContext = challengeService.consumeChallenge(request.transactionId);
        if (challengeContext == null) {
            throw new ResponseStatusException(HttpStatus.BAD_REQUEST, "Challenge expired or invalid");
        }

        byte[] credentialId = Base64.getUrlDecoder().decode(request.id);
        PasskeyCredential_command passkey = credentialRepository.findByCredentialId(credentialId)
                .orElseThrow(() -> new ResponseStatusException(HttpStatus.NOT_FOUND, "Credential not found"));

        if (challengeContext.expectedUserId() != null
                && !challengeContext.expectedUserId().equals(passkey.getUser().getId())) {
            throw new ResponseStatusException(HttpStatus.UNAUTHORIZED, "Passkey does not match this account");
        }
        if (passkey.getAttestedCredentialData() == null) {
            // Records created by the incomplete initial implementation lack the
            // required COSE credential record and must be re-enrolled.
            throw new ResponseStatusException(HttpStatus.CONFLICT, "This passkey must be registered again");
        }

        byte[] clientDataJSON = Base64.getUrlDecoder().decode(request.response.clientDataJSON);
        byte[] authenticatorData = Base64.getUrlDecoder().decode(request.response.authenticatorData);
        byte[] signature = Base64.getUrlDecoder().decode(request.response.signature);

        Origin origin = new Origin(frontendUrl);
        Challenge challenge = new DefaultChallenge(challengeContext.challenge());
        ServerProperty serverProperty = new ServerProperty(origin, rpId, challenge, null);

        AuthenticationRequest authenticationRequest = new AuthenticationRequest(
                credentialId,
                authenticatorData,
                clientDataJSON,
                signature
        );

        AuthenticationData authenticationData = webAuthnManager.parse(authenticationRequest);
        CredentialRecord credentialRecord = toCredentialRecord(passkey);
        AuthenticationParameters authenticationParameters = new AuthenticationParameters(
                serverProperty,
                credentialRecord,
                List.of(credentialId),
                true,
                true
        );
        webAuthnManager.verify(authenticationData, authenticationParameters);

        // WebAuthn4J performs clone/counter validation before this point.
        passkey.setSignCount(authenticationData.getAuthenticatorData().getSignCount());
        byte flags = authenticationData.getAuthenticatorData().getFlags();
        passkey.setBackedUp((flags & 0x10) != 0);
        passkey.setLastUsedAt(Instant.now());
        credentialRepository.save(passkey);

        // Issue JWT — passkey login satisfies MFA
        AppUserDetails userDetails = (AppUserDetails) userDetailsService.loadUserByUsername(passkey.getUser().getEmailId());
        return jwtService.issueTokenPair(passkey.getUser(), userDetails, userAgent, clientIp, deviceId, deviceLabel, false);
    }

    private CredentialRecord toCredentialRecord(PasskeyCredential_command passkey) {
        var attestedCredentialData = new AttestedCredentialDataConverter(new ObjectConverter())
                .convert(passkey.getAttestedCredentialData());
        return new CredentialRecordImpl(
                null,
                passkey.isUserVerificationInitialized(),
                passkey.isBackupEligible(),
                passkey.isBackedUp(),
                passkey.getSignCount(),
                attestedCredentialData,
                null,
                null,
                null,
                null
        );
    }
}
