package com.crescendo.security.webauthn;

import com.fasterxml.jackson.annotation.JsonProperty;
import java.util.List;

public class WebAuthnDTOs {

    public static class RegistrationStartRequest {
        public String credentialName;
    }

    public static class RegistrationStartResponse {
        public Rp rp;
        public User user;
        public String challenge;
        public String transactionId;
        public String verificationSessionId;
        public List<PubKeyCredParam> pubKeyCredParams;
        public AuthenticatorSelection authenticatorSelection;
        public List<ExcludeCredential> excludeCredentials;
        public int timeout;
        public String attestation;

        public static class ExcludeCredential {
            public String type = "public-key";
            public String id;
            public String[] transports;
        }

        public static class Rp {
            public String name;
            public String id;
        }

        public static class User {
            public String id; // Base64URL encoded UUID
            public String name;
            public String displayName;
        }

        public static class PubKeyCredParam {
            public String type = "public-key";
            public int alg; // e.g. -7 for ES256, -257 for RS256
            public PubKeyCredParam(int alg) { this.alg = alg; }
        }

        public static class AuthenticatorSelection {
            public String residentKey = "required";
            public String userVerification = "required";
            public boolean requireResidentKey = true;
        }
    }

    public static class RegistrationFinishRequest {
        public String transactionId;
        public String credentialName;
        public String recoveryToken;
        public String id;
        public String rawId;
        public Response response;
        public String type;

        public static class Response {
            public String clientDataJSON;
            public String attestationObject;
            public String[] transports;
        }
    }

    public static class RecoveryMagicLinkRequest {
        public String email;
    }

    public static class RecoveryRegistrationStartRequest {
        public String recoveryToken;
        public String credentialName;
    }

    public static class PasswordlessStartRequest {
        public String email;
        public String username;
    }

    public static class PasswordlessVerifyRequest {
        public String email;
        public String otp;
    }

    public static class PasswordlessFinishRequest extends RegistrationFinishRequest {
        public String verificationSessionId;
    }
    
    public static class LoginStartRequest {
        public String email; // optional, empty for discoverable credentials
    }

    public static class LoginStartResponse {
        public String challenge;
        public String transactionId;
        public int timeout;
        public String rpId;
        public String userVerification = "required";
        public List<AllowCredential> allowCredentials;

        public static class AllowCredential {
            public String type = "public-key";
            public String id;
            public String[] transports;
        }
    }

    public static class LoginFinishRequest {
        public String transactionId;
        public String id;
        public String rawId;
        public Response response;
        public String type;
        // Retained only for backwards-compatible clients. Account binding comes
        // from the server-side transaction, never from this client input.
        public String email;

        public static class Response {
            public String clientDataJSON;
            public String authenticatorData;
            public String signature;
            public String userHandle;
        }
    }

    public record CredentialResponse(
            String id,
            String name,
            String[] transports,
            boolean backupEligible,
            boolean backedUp,
            java.time.Instant createdAt,
            java.time.Instant lastUsedAt
    ) {}
}
