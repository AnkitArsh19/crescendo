package com.crescendo.apps.crypto;

import com.crescendo.execution.action.ActionContext;
import com.crescendo.execution.action.ActionHandler;
import com.crescendo.execution.action.ActionMapping;
import com.crescendo.execution.action.ActionResult;
import org.springframework.stereotype.Component;

import javax.crypto.Cipher;
import javax.crypto.spec.SecretKeySpec;
import java.nio.charset.StandardCharsets;
import java.util.Base64;
import java.util.Map;

@Component
@ActionMapping(appKey = "crypto", actionKey = "decrypt")
public class CryptoDecryptActionHandler implements ActionHandler {
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
            Cipher cipher = Cipher.getInstance("AES/ECB/PKCS5Padding");
            cipher.init(Cipher.DECRYPT_MODE, secretKeySpec);
            
            byte[] decodedBytes = Base64.getDecoder().decode(value);
            byte[] decryptedBytes = cipher.doFinal(decodedBytes);
            String decryptedString = new String(decryptedBytes, StandardCharsets.UTF_8);
            
            return ActionResult.success(Map.of("result", decryptedString));
        } catch (Exception e) {
            return ActionResult.failure("Decryption failed: " + e.getMessage());
        }
    }
}
