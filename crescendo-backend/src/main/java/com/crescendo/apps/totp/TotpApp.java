package com.crescendo.apps.totp;

import com.crescendo.app.App;
import com.crescendo.apps.AppDefinition;
import com.crescendo.enums.AuthType;
import org.springframework.stereotype.Component;

import java.util.List;
import java.util.Map;

@Component
public class TotpApp implements AppDefinition {
    @Override
    public App toApp() {
        return new App("totp", "TOTP", "Generate and verify time-based one-time passcodes",
                "/icons/totp.svg", AuthType.NONE,
                List.of(),
                List.of(
                        Map.of("actionKey", "generate", "name", "Generate Code",
                                "description", "Generate a TOTP code from a Base32 secret",
                                "configSchema", schema(false)),
                        Map.of("actionKey", "verify", "name", "Verify Code",
                                "description", "Verify a TOTP code from a Base32 secret",
                                "configSchema", schema(true))
                )
        ).credentialSchema(List.of()).category("core").helpUrl("");
    }

    private List<Map<String, Object>> schema(boolean includeCode) {
        java.util.ArrayList<Map<String, Object>> fields = new java.util.ArrayList<>();
        fields.add(Map.of("key", "secret", "label", "Base32 Secret", "type", "password", "required", true,
                "placeholder", "JBSWY3DPEHPK3PXP", "helpText", "Shared TOTP secret"));
        if (includeCode) {
            fields.add(Map.of("key", "code", "label", "Code", "type", "text", "required", true,
                    "placeholder", "123456", "helpText", "Code to verify"));
        }
        fields.add(Map.of("key", "digits", "label", "Digits", "type", "number", "required", false,
                "placeholder", "6", "helpText", "Number of code digits"));
        fields.add(Map.of("key", "period", "label", "Period Seconds", "type", "number", "required", false,
                "placeholder", "30", "helpText", "TOTP time step"));
        return fields;
    }
}
