package com.crescendo.apps.crypto;

import com.crescendo.execution.action.ActionContext;
import com.crescendo.execution.action.ActionMapping;
import org.springframework.stereotype.Component;

import javax.crypto.Cipher;
import javax.crypto.Mac;
import javax.crypto.SecretKey;
import javax.crypto.SecretKeyFactory;
import javax.crypto.spec.GCMParameterSpec;
import javax.crypto.spec.PBEKeySpec;
import javax.crypto.spec.SecretKeySpec;
import java.nio.charset.StandardCharsets;
import java.security.MessageDigest;
import java.security.SecureRandom;
import java.security.spec.KeySpec;
import java.util.Base64;
import java.util.HexFormat;
import java.util.LinkedHashMap;
import java.util.Map;
import java.util.UUID;

/**
 * Production Crypto action handlers for appKey="crypto".
 */
@Component
public class CryptoHandlers {

    private static final SecureRandom RANDOM = new SecureRandom();

    @ActionMapping(appKey = "crypto", actionKey = "crypto:hash")
    public Object hash(ActionContext context) throws Exception {
        String type = context.getString("type");
        if (type == null || type.isBlank()) type = "SHA-256";
        String value = context.getString("value");
        if (value == null) value = "";
        String encoding = context.getString("encoding");
        if (encoding == null || encoding.isBlank()) encoding = "hex";
        String dataPropertyName = context.getString("dataPropertyName");
        if (dataPropertyName == null || dataPropertyName.isBlank()) dataPropertyName = "data";

        String algo = normalizeHashAlgo(type);
        MessageDigest digest = MessageDigest.getInstance(algo);
        byte[] hash = digest.digest(value.getBytes(StandardCharsets.UTF_8));

        String result = "base64".equalsIgnoreCase(encoding)
                ? Base64.getEncoder().encodeToString(hash)
                : HexFormat.of().formatHex(hash);

        Map<String, Object> out = new LinkedHashMap<>();
        out.put(dataPropertyName, result);
        out.put("algorithm", algo);
        out.put("encoding", encoding);
        return out;
    }

    @ActionMapping(appKey = "crypto", actionKey = "crypto:hmac")
    public Object hmac(ActionContext context) throws Exception {
        String type = context.getString("type");
        if (type == null || type.isBlank()) type = "SHA256";
        String value = context.getString("value");
        if (value == null) value = "";
        String secret = context.getCredential("hmacSecret");
        if (secret == null || secret.isBlank()) secret = context.getString("secret");
        if (secret == null) secret = "";
        String encoding = context.getString("encoding");
        if (encoding == null || encoding.isBlank()) encoding = "hex";
        String dataPropertyName = context.getString("dataPropertyName");
        if (dataPropertyName == null || dataPropertyName.isBlank()) dataPropertyName = "data";

        String algo = "Hmac" + type.replace("-", "").toUpperCase();
        Mac mac = Mac.getInstance(algo);
        SecretKeySpec keySpec = new SecretKeySpec(secret.getBytes(StandardCharsets.UTF_8), algo);
        mac.init(keySpec);
        byte[] rawHmac = mac.doFinal(value.getBytes(StandardCharsets.UTF_8));

        String result = "base64".equalsIgnoreCase(encoding)
                ? Base64.getEncoder().encodeToString(rawHmac)
                : HexFormat.of().formatHex(rawHmac);

        Map<String, Object> out = new LinkedHashMap<>();
        out.put(dataPropertyName, result);
        out.put("algorithm", algo);
        out.put("encoding", encoding);
        return out;
    }

    @ActionMapping(appKey = "crypto", actionKey = "crypto:generate")
    public Object generate(ActionContext context) throws Exception {
        String encodingType = context.getString("encodingType");
        int length = context.getInt("stringLength", 32);
        String dataPropertyName = context.getString("dataPropertyName");
        if (dataPropertyName == null || dataPropertyName.isBlank()) dataPropertyName = "data";

        String result;
        if ("uuid".equalsIgnoreCase(encodingType)) {
            result = UUID.randomUUID().toString();
        } else if ("hex".equalsIgnoreCase(encodingType)) {
            byte[] bytes = new byte[Math.max(1, length / 2)];
            RANDOM.nextBytes(bytes);
            result = HexFormat.of().formatHex(bytes);
        } else if ("base64".equalsIgnoreCase(encodingType)) {
            byte[] bytes = new byte[Math.max(1, length)];
            RANDOM.nextBytes(bytes);
            result = Base64.getUrlEncoder().withoutPadding().encodeToString(bytes);
        } else {
            // alphanumeric
            final String chars = "ABCDEFGHIJKLMNOPQRSTUVWXYZabcdefghijklmnopqrstuvwxyz0123456789";
            StringBuilder sb = new StringBuilder(length);
            for (int i = 0; i < length; i++) {
                sb.append(chars.charAt(RANDOM.nextInt(chars.length())));
            }
            result = sb.toString();
        }

        Map<String, Object> out = new LinkedHashMap<>();
        out.put(dataPropertyName, result);
        return out;
    }

    @ActionMapping(appKey = "crypto", actionKey = "crypto:encrypt")
    public Object encrypt(ActionContext context) throws Exception {
        String value = context.getString("value");
        if (value == null) value = "";
        String passphrase = context.getCredential("passphrase");
        if (passphrase == null || passphrase.isBlank()) passphrase = context.getString("passphrase");
        if (passphrase == null || passphrase.isBlank()) passphrase = "crescendo-default-key";

        String dataPropertyName = context.getString("dataPropertyName");
        if (dataPropertyName == null || dataPropertyName.isBlank()) dataPropertyName = "data";

        byte[] salt = new byte[16];
        byte[] iv = new byte[12];
        RANDOM.nextBytes(salt);
        RANDOM.nextBytes(iv);

        SecretKeyFactory factory = SecretKeyFactory.getInstance("PBKDF2WithHmacSHA256");
        KeySpec spec = new PBEKeySpec(passphrase.toCharArray(), salt, 65536, 256);
        SecretKey tmp = factory.generateSecret(spec);
        SecretKeySpec secretKey = new SecretKeySpec(tmp.getEncoded(), "AES");

        Cipher cipher = Cipher.getInstance("AES/GCM/NoPadding");
        cipher.init(Cipher.ENCRYPT_MODE, secretKey, new GCMParameterSpec(128, iv));
        byte[] encrypted = cipher.doFinal(value.getBytes(StandardCharsets.UTF_8));

        // Format: salt:iv:ciphertext in base64
        String payload = Base64.getEncoder().encodeToString(salt) + ":"
                + Base64.getEncoder().encodeToString(iv) + ":"
                + Base64.getEncoder().encodeToString(encrypted);

        Map<String, Object> out = new LinkedHashMap<>();
        out.put(dataPropertyName, payload);
        out.put("mode", "AES-GCM");
        return out;
    }

    @ActionMapping(appKey = "crypto", actionKey = "crypto:decrypt")
    public Object decrypt(ActionContext context) throws Exception {
        String value = context.getString("value");
        if (value == null) value = "";
        String passphrase = context.getCredential("passphrase");
        if (passphrase == null || passphrase.isBlank()) passphrase = context.getString("passphrase");
        if (passphrase == null || passphrase.isBlank()) passphrase = "crescendo-default-key";

        String dataPropertyName = context.getString("dataPropertyName");
        if (dataPropertyName == null || dataPropertyName.isBlank()) dataPropertyName = "data";

        String[] parts = value.split(":");
        if (parts.length != 3) {
            throw new IllegalArgumentException("Invalid encrypted payload format (expected salt:iv:ciphertext)");
        }

        byte[] salt = Base64.getDecoder().decode(parts[0]);
        byte[] iv = Base64.getDecoder().decode(parts[1]);
        byte[] ciphertext = Base64.getDecoder().decode(parts[2]);

        SecretKeyFactory factory = SecretKeyFactory.getInstance("PBKDF2WithHmacSHA256");
        KeySpec spec = new PBEKeySpec(passphrase.toCharArray(), salt, 65536, 256);
        SecretKey tmp = factory.generateSecret(spec);
        SecretKeySpec secretKey = new SecretKeySpec(tmp.getEncoded(), "AES");

        Cipher cipher = Cipher.getInstance("AES/GCM/NoPadding");
        cipher.init(Cipher.DECRYPT_MODE, secretKey, new GCMParameterSpec(128, iv));
        byte[] decrypted = cipher.doFinal(ciphertext);

        Map<String, Object> out = new LinkedHashMap<>();
        out.put(dataPropertyName, new String(decrypted, StandardCharsets.UTF_8));
        return out;
    }

    private static String normalizeHashAlgo(String type) {
        String clean = type.toUpperCase().replace("-", "");
        return switch (clean) {
            case "MD5" -> "MD5";
            case "SHA1" -> "SHA-1";
            case "SHA256" -> "SHA-256";
            case "SHA384" -> "SHA-384";
            case "SHA512" -> "SHA-512";
            default -> "SHA-256";
        };
    }
}
