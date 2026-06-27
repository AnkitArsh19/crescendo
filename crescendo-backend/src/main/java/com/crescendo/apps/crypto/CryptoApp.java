package com.crescendo.apps.crypto;

import com.crescendo.app.App;
import com.crescendo.apps.AppDefinition;
import com.crescendo.enums.AuthType;
import org.springframework.stereotype.Component;

import java.util.List;
import java.util.Map;

/**
 * AppDefinition for Crypto.
 */
@Component
public class CryptoApp implements AppDefinition {

    @Override
    public App toApp() {
        return new App(
                "crypto",
                "Crypto",
                """
                Provide cryptographic utilities.
                
                This integration provides operations for:
                - **Decrypt**: Decrypt a string with a passphrase or private key
                - **Encrypt**: Encrypt a string with a passphrase or public key
                - **Generate**: Generate random string
                - **Hash**: Hash a text or file in a specified format
                - **Hmac**: HMAC a text or file in a specified format
                - **Sign**: Sign a string using a private key
                """,
                "https://www.google.com/s2/favicons?domain=crypto.com&sz=128", // Generic icon
                AuthType.APIKEY, // Uses custom credentials map
                List.of(),
                List.of(
                        Map.of(
                                "actionKey", "crypto:hash",
                                "name", "Hash",
                                "description", "Hash a text or file in a specified format",
                                "configSchema", List.of(
                                        Map.of("key", "binaryData", "label", "Binary File", "type", "boolean", "default", false),
                                        Map.of("key", "binaryPropertyName", "label", "Binary Property Name", "type", "text", "default", "data"),
                                        Map.of("key", "type", "label", "Type", "type", "text", "default", "SHA256"),
                                        Map.of("key", "value", "label", "Value", "type", "text"),
                                        Map.of("key", "dataPropertyName", "label", "Property Name", "type", "text", "default", "data"),
                                        Map.of("key", "encoding", "label", "Encoding", "type", "text", "default", "hex")
                                )
                        ),
                        Map.of(
                                "actionKey", "crypto:hmac",
                                "name", "HMAC",
                                "description", "HMAC a text or file in a specified format",
                                "configSchema", List.of(
                                        Map.of("key", "binaryData", "label", "Binary File", "type", "boolean", "default", false),
                                        Map.of("key", "binaryPropertyName", "label", "Binary Property Name", "type", "text", "default", "data"),
                                        Map.of("key", "type", "label", "Type", "type", "text", "default", "SHA256"),
                                        Map.of("key", "value", "label", "Value", "type", "text"),
                                        Map.of("key", "dataPropertyName", "label", "Property Name", "type", "text", "default", "data"),
                                        Map.of("key", "encoding", "label", "Encoding", "type", "text", "default", "hex")
                                )
                        ),
                        Map.of(
                                "actionKey", "crypto:sign",
                                "name", "Sign",
                                "description", "Sign a string using a private key",
                                "configSchema", List.of(
                                        Map.of("key", "value", "label", "Value", "type", "text", "required", true),
                                        Map.of("key", "dataPropertyName", "label", "Property Name", "type", "text", "default", "data"),
                                        Map.of("key", "algorithm", "label", "Algorithm Name or ID", "type", "text", "required", true),
                                        Map.of("key", "encoding", "label", "Encoding", "type", "text", "default", "hex")
                                )
                        ),
                        Map.of(
                                "actionKey", "crypto:generate",
                                "name", "Generate",
                                "description", "Generate random string",
                                "configSchema", List.of(
                                        Map.of("key", "dataPropertyName", "label", "Property Name", "type", "text", "default", "data"),
                                        Map.of("key", "encodingType", "label", "Type", "type", "text", "default", "uuid"),
                                        Map.of("key", "stringLength", "label", "Length", "type", "number", "default", 32)
                                )
                        ),
                        Map.of(
                                "actionKey", "crypto:encrypt",
                                "name", "Encrypt",
                                "description", "Encrypt a string with a passphrase or public key",
                                "configSchema", List.of(
                                        Map.of("key", "mode", "label", "Mode", "type", "text", "default", "symmetric"),
                                        Map.of("key", "cipher", "label", "Cipher", "type", "text", "default", "aes-256-gcm"),
                                        Map.of("key", "value", "label", "Value", "type", "text", "required", true),
                                        Map.of("key", "dataPropertyName", "label", "Property Name", "type", "text", "default", "data")
                                )
                        ),
                        Map.of(
                                "actionKey", "crypto:decrypt",
                                "name", "Decrypt",
                                "description", "Decrypt a string with a passphrase or private key",
                                "configSchema", List.of(
                                        Map.of("key", "mode", "label", "Mode", "type", "text", "default", "symmetric"),
                                        Map.of("key", "cipher", "label", "Cipher", "type", "text", "default", "aes-256-gcm"),
                                        Map.of("key", "value", "label", "Value", "type", "text", "required", true),
                                        Map.of("key", "dataPropertyName", "label", "Property Name", "type", "text", "default", "data")
                                )
                        )
                )
        ).credentialSchema(List.of(
                Map.of("key", "hmacSecret", "label", "HMAC Secret", "type", "password"),
                Map.of("key", "signPrivateKey", "label", "Sign Private Key", "type", "password"),
                Map.of("key", "encryptionPassphrase", "label", "Encryption Passphrase", "type", "password"),
                Map.of("key", "encryptionPublicKey", "label", "Encryption Public Key", "type", "password"),
                Map.of("key", "encryptionPrivateKey", "label", "Encryption Private Key", "type", "password")
        )).category("data-transformation");
    }
}
