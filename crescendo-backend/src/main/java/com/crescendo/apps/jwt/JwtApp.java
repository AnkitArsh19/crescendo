package com.crescendo.apps.jwt;

import com.crescendo.app.App;
import com.crescendo.apps.AppDefinition;
import com.crescendo.enums.AuthType;
import org.springframework.stereotype.Component;

import java.util.List;
import java.util.Map;

@Component
public class JwtApp implements AppDefinition {
    @Override
    public App toApp() {
        return new App("jwt", "JWT", "Sign and Verify JSON Web Tokens",
                "/icons/jwt.svg", AuthType.NONE,
                List.of(),
                List.of(
                    Map.of("actionKey", "sign", "name", "Sign",
                        "description", "Create a new JWT from a payload",
                        "configSchema", List.of(
                            Map.of("key", "secret", "label", "Secret Key", "type", "text", "required", true,
                                   "placeholder", "your-256-bit-secret", "helpText", "The secret used to sign the token"),
                            Map.of("key", "payload", "label", "Payload (JSON)", "type", "json", "required", true,
                                   "placeholder", "{\"sub\": \"1234567890\", \"name\": \"John Doe\"}", "helpText", "The JSON payload to sign"),
                            Map.of("key", "expiresIn", "label", "Expires In (seconds)", "type", "number", "required", false,
                                   "placeholder", "3600", "helpText", "Expiration time in seconds from now (optional)")
                        )),
                    Map.of("actionKey", "verify", "name", "Verify",
                        "description", "Verify a JWT and extract its payload",
                        "configSchema", List.of(
                            Map.of("key", "token", "label", "Token", "type", "text", "required", true,
                                   "placeholder", "eyJhbGciOiJIUzI1NiIsInR5cCI6IkpXVCJ9...", "helpText", "The JWT to verify"),
                            Map.of("key", "secret", "label", "Secret Key", "type", "text", "required", true,
                                   "placeholder", "your-256-bit-secret", "helpText", "The secret used to verify the signature")
                        ))
                )
        ).credentialSchema(List.of()).category("core").helpUrl("");
    }
}
