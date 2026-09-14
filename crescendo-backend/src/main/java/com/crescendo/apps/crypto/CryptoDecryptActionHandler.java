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
import java.util.Arrays;
import java.util.Base64;
import java.util.Map;

@Component
@ActionMapping(appKey = "crypto", actionKey = "decrypt")
public class CryptoDecryptActionHandler implements ActionHandler {
    private static final int GCM_IV_LENGTH_BYTES = 12;
    private static final int GCM_TAG_LENGTH_BITS = 128;

    @Override
    public ActionResult execute(ActionContext context) {
        String value = String.valueOf(context.configuration().getOrDefault("value", ""));
        String secret = String.valueOf(context.configuration().getOrDefault("secret", ""));
        
        if (value.isBlank() || secret.isBlank()) {
            return ActionResult.failure("Decrypt requires both value and secret");
        }
        
        try {
            // Pad or truncate secret to 16 bytes for AES-128 (simple default approach)
            byte[] keyBytes = secret.getBytes(StandardCharsets.UTF_8);
            byte[] finalKey = new byte[16];
            System.arraycopy(keyBytes, 0, finalKey, 0, Math.min(keyBytes.length, 16));
            
            SecretKeySpec secretKeySpec = new SecretKeySpec(finalKey, "AES");
            Cipher cipher = Cipher.getInstance("AES/GCM/NoPadding");
            
            byte[] payload = Base64.getDecoder().decode(value);
            if (payload.length <= GCM_IV_LENGTH_BYTES) {
                return ActionResult.failure("Decryption failed: Invalid encrypted payload");
            }
            byte[] iv = Arrays.copyOfRange(payload, 0, GCM_IV_LENGTH_BYTES);
            byte[] decodedBytes = Arrays.copyOfRange(payload, GCM_IV_LENGTH_BYTES, payload.length);
            cipher.init(Cipher.DECRYPT_MODE, secretKeySpec, new GCMParameterSpec(GCM_TAG_LENGTH_BITS, iv));
            byte[] decryptedBytes = cipher.doFinal(decodedBytes);
            String decryptedString = new String(decryptedBytes, StandardCharsets.UTF_8);
            
            return ActionResult.success(Map.of("result", decryptedString));
        } catch (Exception e) {
            return ActionResult.failure("Decryption failed: " + e.getMessage());
        }
    }
}
