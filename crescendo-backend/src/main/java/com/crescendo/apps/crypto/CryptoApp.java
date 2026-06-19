package com.crescendo.apps.crypto;

import com.crescendo.app.App;
import com.crescendo.apps.AppDefinition;
import com.crescendo.enums.AuthType;
import org.springframework.stereotype.Component;

import java.util.List;
import java.util.Map;

@Component
public class CryptoApp implements AppDefinition {
    @Override
    public App toApp() {
        return new App("crypto", "Crypto", "Cryptographic operations like Hash and HMAC",
                "/icons/crypto.svg", AuthType.NONE,
                List.of(),
                List.of(
                    Map.of("actionKey", "hash", "name", "Hash",
                        "description", "Generate a hash from text",
                        "configSchema", List.of(
                            Map.of("key", "algorithm", "label", "Algorithm", "type", "select", "required", true,
                                   "options", List.of(
                                       Map.of("value", "MD5", "label", "MD5"),
                                       Map.of("value", "SHA-1", "label", "SHA-1"),
                                       Map.of("value", "SHA-256", "label", "SHA-256"),
                                       Map.of("value", "SHA-512", "label", "SHA-512")
                                   ), "helpText", "Hash algorithm to use"),
                            Map.of("key", "value", "label", "Value", "type", "text", "required", true,
                                   "placeholder", "my_secret_data", "helpText", "The data to hash")
                        )),
                    Map.of("actionKey", "hmac", "name", "HMAC",
                        "description", "Generate a Hash-based Message Authentication Code",
                        "configSchema", List.of(
                            Map.of("key", "algorithm", "label", "Algorithm", "type", "select", "required", true,
                                   "options", List.of(
                                       Map.of("value", "HmacSHA256", "label", "HMAC-SHA256"),
                                       Map.of("value", "HmacSHA512", "label", "HMAC-SHA512")
                                   ), "helpText", "HMAC algorithm to use"),
                            Map.of("key", "value", "label", "Value", "type", "text", "required", true,
                                   "placeholder", "data_to_sign", "helpText", "The data to authenticate"),
                            Map.of("key", "secret", "label", "Secret Key", "type", "text", "required", true,
                                   "placeholder", "my_secret_key", "helpText", "The key for the HMAC operation")
                        ))
                )
        ).credentialSchema(List.of()).category("core").helpUrl("");
    }
}
