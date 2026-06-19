package com.crescendo.apps.crypto;

import com.crescendo.execution.action.ActionContext;
import com.crescendo.execution.action.ActionHandler;
import com.crescendo.execution.action.ActionMapping;
import com.crescendo.execution.action.ActionResult;
import org.springframework.stereotype.Component;

import java.nio.charset.StandardCharsets;
import java.security.MessageDigest;
import java.util.Map;

@Component
@ActionMapping(appKey = "crypto", actionKey = "hash")
public class CryptoHashActionHandler implements ActionHandler {

    @Override
public ActionResult execute(ActionContext context) {
        Map<String, Object> config = context.configuration();
        if (config == null || !config.containsKey("algorithm") || !config.containsKey("value")) {
            return ActionResult.failure("Hash action requires 'algorithm' and 'value' in configuration");
        }

        String algorithm = String.valueOf(config.get("algorithm"));
        String value = String.valueOf(config.get("value"));

        try {
            MessageDigest digest = MessageDigest.getInstance(algorithm);
            byte[] hashBytes = digest.digest(value.getBytes(StandardCharsets.UTF_8));
            
            // Convert to hex string
            StringBuilder hexString = new StringBuilder();
            for (byte b : hashBytes) {
                String hex = Integer.toHexString(0xff & b);
                if (hex.length() == 1) hexString.append('0');
                hexString.append(hex);
            }
            
            return ActionResult.success(Map.of("hash", hexString.toString()));
        } catch (Exception e) {
            return ActionResult.failure("Failed to compute hash: " + e.getMessage());
        }
    }
}
