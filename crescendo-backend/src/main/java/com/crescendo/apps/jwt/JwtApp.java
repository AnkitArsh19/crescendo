package com.crescendo.apps.jwt;

import com.crescendo.app.App;
import com.crescendo.apps.AppDefinition;
import com.crescendo.enums.AuthType;
import org.springframework.stereotype.Component;

import java.util.List;
import java.util.Map;

/**
 * AppDefinition for JWT.
 */
@Component
public class JwtApp implements AppDefinition {

    @Override
    public App toApp() {
        return new App(
                "jwt",
                "JWT",
                """
                JSON Web Token (JWT) is a standard for creating data with optional signature and/or optional encryption whose payload holds JSON that asserts some number of claims.
                
                This integration provides operations for:
                - **Sign**: Create a new signed JWT
                - **Verify**: Verify the signature of a JWT
                - **Decode**: Decode a JWT without verifying the signature
                
                Authenticate using a secret passphrase or PEM encoded public/private keys.
                """,
                "https://www.google.com/s2/favicons?domain=jwt.io&sz=128", // Generic icon
                AuthType.APIKEY, // Uses custom credentials map
                List.of(),
                List.of(
                        Map.of(
                                "actionKey", "jwt:sign",
                                "name", "Sign a JWT",
                                "description", "Sign a JWT",
                                "configSchema", List.of(
                                        Map.of("key", "useJson", "label", "Use JSON to Build Payload", "type", "boolean", "default", false),
                                        Map.of("key", "claims", "label", "Payload Claims", "type", "json"),
                                        Map.of("key", "claimsJson", "label", "Payload Claims (JSON)", "type", "json"),
                                        Map.of("key", "headerClaims", "label", "Header Claims (JSON)", "type", "json"),
                                        Map.of("key", "options", "label", "Options", "type", "json")
                                )
                        ),
                        Map.of(
                                "actionKey", "jwt:verify",
                                "name", "Verify a JWT",
                                "description", "Verify a JWT",
                                "configSchema", List.of(
                                        Map.of("key", "token", "label", "Token", "type", "text", "required", true),
                                        Map.of("key", "options", "label", "Options", "type", "json")
                                )
                        ),
                        Map.of(
                                "actionKey", "jwt:decode",
                                "name", "Decode a JWT",
                                "description", "Decode a JWT without verifying the signature",
                                "configSchema", List.of(
                                        Map.of("key", "token", "label", "Token", "type", "text", "required", true),
                                        Map.of("key", "options", "label", "Options", "type", "json")
                                )
                        )
                )
        ).credentialSchema(List.of(
                Map.of("key", "keyType", "label", "Key Type", "type", "text", "default", "passphrase"),
                Map.of("key", "secret", "label", "Secret", "type", "password"),
                Map.of("key", "privateKey", "label", "Private Key", "type", "password"),
                Map.of("key", "publicKey", "label", "Public Key", "type", "password"),
                Map.of("key", "algorithm", "label", "Algorithm", "type", "text", "default", "HS256")
        )).category("developer-tools");
    }
}
