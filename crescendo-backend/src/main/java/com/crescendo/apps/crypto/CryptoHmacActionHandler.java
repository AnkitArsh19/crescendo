package com.crescendo.apps.crypto;

import com.crescendo.execution.action.ActionContext;
import com.crescendo.execution.action.ActionHandler;
import com.crescendo.execution.action.ActionMapping;
import com.crescendo.execution.action.ActionResult;
import org.springframework.stereotype.Component;

import javax.crypto.Mac;
import javax.crypto.spec.SecretKeySpec;
import java.nio.charset.StandardCharsets;
import java.util.Map;

@Component
@ActionMapping(appKey = "crypto", actionKey = "hmac")
public class CryptoHmacActionHandler implements ActionHandler {

    @Override
public ActionResult execute(ActionContext context) {
        Map<String, Object> config = context.configuration();
        if (config == null || !config.containsKey("algorithm") || !config.containsKey("value") || !config.containsKey("secret")) {
            return ActionResult.failure("HMAC action requires 'algorithm', 'value', and 'secret' in configuration");
        }

        String algorithm = String.valueOf(config.get("algorithm"));
        String value = String.valueOf(config.get("value"));
        String secret = String.valueOf(config.get("secret"));

        try {
            Mac mac = Mac.getInstance(algorithm);
            SecretKeySpec secretKeySpec = new SecretKeySpec(secret.getBytes(StandardCharsets.UTF_8), algorithm);
            mac.init(secretKeySpec);
            
            byte[] hmacBytes = mac.doFinal(value.getBytes(StandardCharsets.UTF_8));
            
            // Convert to hex string
            StringBuilder hexString = new StringBuilder();
            for (byte b : hmacBytes) {
                String hex = Integer.toHexString(0xff & b);
                if (hex.length() == 1) hexString.append('0');
                hexString.append(hex);
            }
            
            return ActionResult.success(Map.of("hmac", hexString.toString()));
        } catch (Exception e) {
            return ActionResult.failure("Failed to compute HMAC: " + e.getMessage());
        }
    }
}
