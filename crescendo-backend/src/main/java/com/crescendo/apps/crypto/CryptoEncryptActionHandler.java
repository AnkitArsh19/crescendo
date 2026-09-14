package com.crescendo.apps.crypto;

import com.crescendo.execution.action.ActionContext;
import com.crescendo.execution.action.ActionHandler;
import com.crescendo.execution.action.ActionMapping;
import com.crescendo.execution.action.ActionResult;
import org.springframework.stereotype.Component;

import javax.crypto.Cipher;
import javax.crypto.spec.GCMParameterSpec;
import javax.crypto.spec.SecretKeySpec;
import java.nio.charset.StandardCharsets;
import java.security.SecureRandom;
import java.util.Arrays;
import java.util.Base64;
import java.util.Map;

@Component
@ActionMapping(appKey = "crypto", actionKey = "encrypt")
public class CryptoEncryptActionHandler implements ActionHandler {
    private static final int GCM_IV_LENGTH_BYTES = 12;
    private static final int GCM_TAG_LENGTH_BITS = 128;

    @Override
    public ActionResult execute(ActionContext context) {
        String value = String.valueOf(context.configuration().getOrDefault("value", ""));
        String secret = String.valueOf(context.configuration().getOrDefault("secret", ""));
        
        if (value.isBlank() || secret.isBlank()) {
            return ActionResult.failure("Encrypt requires both value and secret");
        }
        
        try {
            // Pad or truncate secret to 16 bytes for AES-128 (simple default approach)
            byte[] keyBytes = secret.getBytes(StandardCharsets.UTF_8);
            byte[] finalKey = new byte[16];
            System.arraycopy(keyBytes, 0, finalKey, 0, Math.min(keyBytes.length, 16));
            
            SecretKeySpec secretKeySpec = new SecretKeySpec(finalKey, "AES");
            Cipher cipher = Cipher.getInstance("AES/GCM/NoPadding");
            byte[] iv = new byte[GCM_IV_LENGTH_BYTES];
            new SecureRandom().nextBytes(iv);
            cipher.init(Cipher.ENCRYPT_MODE, secretKeySpec, new GCMParameterSpec(GCM_TAG_LENGTH_BITS, iv));
            
            byte[] encryptedBytes = cipher.doFinal(value.getBytes(StandardCharsets.UTF_8));
            byte[] payload = Arrays.copyOf(iv, iv.length + encryptedBytes.length);
            System.arraycopy(encryptedBytes, 0, payload, iv.length, encryptedBytes.length);
            String base64Encrypted = Base64.getEncoder().encodeToString(payload);
            
            return ActionResult.success(Map.of("result", base64Encrypted));
        } catch (Exception e) {
            return ActionResult.failure("Encryption failed: " + e.getMessage());
        }
    }
}
