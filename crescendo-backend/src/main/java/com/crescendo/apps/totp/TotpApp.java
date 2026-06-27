package com.crescendo.apps.totp;

import com.crescendo.app.App;
import com.crescendo.apps.AppDefinition;
import com.crescendo.enums.AuthType;
import org.springframework.stereotype.Component;

import java.util.List;
import java.util.Map;

/**
 * AppDefinition for TOTP.
 */
@Component
public class TotpApp implements AppDefinition {

    @Override
    public App toApp() {
        return new App(
                "totp",
                "TOTP",
                """
                TOTP (Time-Based One-Time Password) is a standard algorithm that generates a one-time password using the current time as a source of uniqueness.
                
                **What you can do with TOTP in Crescendo:**
                - Generate secure two-factor authentication (2FA) tokens on the fly
                - Programmatically authenticate into external services that enforce 2FA
                - Automate login flows that require TOTP verification
                
                **Actions available:**
                - Generate Secret — generates a time-based one-time password based on your configured TOTP API credentials
                
                **Who should use this:** Security engineers, backend developers, and automation specialists managing 2FA-secured services.
                
                **Authentication:** Requires TOTP API Credentials.
                """,
                "/icons/totp.png", // Generic icon
                AuthType.NONE,
                List.of(
                        Map.of(
                                "key", "totpApi",
                                "label", "TOTP API Credentials",
                                "type", "string",
                                "required", true
                        )
                ),
                List.of(
                        Map.of(
                                "actionKey", "totp:generateSecret",
                                "name", "Generate Secret",
                                "description", "Generate a time-based one-time password",
                                "configSchema", List.of(
                                        Map.of("key", "options", "label", "Options", "type", "json")
                                )
                        )
                )
        ).category("logic-and-flow");
    }
}
