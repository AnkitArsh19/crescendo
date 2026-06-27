package com.crescendo.apps.crypto;

import com.crescendo.execution.action.ActionContext;
import com.crescendo.execution.action.ActionHandler;
import com.crescendo.execution.action.ActionMapping;
import com.crescendo.execution.action.ActionResult;
import org.springframework.stereotype.Component;

import java.security.SecureRandom;
import java.util.Base64;
import java.util.Map;
import java.util.UUID;

@Component
@ActionMapping(appKey = "crypto", actionKey = "generate")
public class CryptoGenerateActionHandler implements ActionHandler {

    private static final SecureRandom RANDOM = new SecureRandom();

    @Override
    public ActionResult execute(ActionContext context) {
        Map<String, Object> config = context.configuration();
        
        String encodingType = config.getOrDefault("encodingType", "uuid").toString();
        int stringLength = config.containsKey("stringLength") ? Integer.parseInt(String.valueOf(config.get("stringLength"))) : 32;
        
        try {
            String newValue;
            if ("uuid".equals(encodingType)) {
                newValue = UUID.randomUUID().toString();
            } else {
                byte[] bytes = new byte[stringLength];
                RANDOM.nextBytes(bytes);
                
                if ("base64".equals(encodingType)) {
                    newValue = Base64.getEncoder().encodeToString(bytes).replaceAll("\\W", "").substring(0, Math.min(stringLength, Base64.getEncoder().encodeToString(bytes).replaceAll("\\W", "").length()));
                } else if ("hex".equals(encodingType)) {
                    StringBuilder sb = new StringBuilder();
                    for (byte b : bytes) {
                        sb.append(String.format("%02x", b));
                    }
                    newValue = sb.toString().substring(0, Math.min(stringLength, sb.length()));
                } else {
                    return ActionResult.failure("Unsupported encoding type: " + encodingType);
                }
            }
            
            return ActionResult.success(Map.of("data", newValue));
        } catch (Exception e) {
            return ActionResult.failure("Failed to generate string: " + e.getMessage());
        }
    }
}
