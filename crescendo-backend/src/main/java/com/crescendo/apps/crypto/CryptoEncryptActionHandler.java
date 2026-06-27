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
@ActionMapping(appKey = "crypto", actionKey = "encrypt")
public class CryptoEncryptActionHandler implements ActionHandler {
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
            Cipher cipher = Cipher.getInstance("AES/ECB/PKCS5Padding");
            cipher.init(Cipher.ENCRYPT_MODE, secretKeySpec);
            
            byte[] encryptedBytes = cipher.doFinal(value.getBytes(StandardCharsets.UTF_8));
            String base64Encrypted = Base64.getEncoder().encodeToString(encryptedBytes);
            
            return ActionResult.success(Map.of("result", base64Encrypted));
        } catch (Exception e) {
            return ActionResult.failure("Encryption failed: " + e.getMessage());
        }
    }
}
