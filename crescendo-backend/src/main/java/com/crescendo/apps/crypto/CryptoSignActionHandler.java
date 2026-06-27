package com.crescendo.apps.crypto;

import com.crescendo.execution.action.ActionContext;
import com.crescendo.execution.action.ActionHandler;
import com.crescendo.execution.action.ActionMapping;
import com.crescendo.execution.action.ActionResult;
import org.springframework.stereotype.Component;

import java.security.KeyFactory;
import java.security.PrivateKey;
import java.security.Signature;
import java.security.spec.PKCS8EncodedKeySpec;
import java.util.Base64;
import java.util.Map;

@Component
@ActionMapping(appKey = "crypto", actionKey = "sign")
public class CryptoSignActionHandler implements ActionHandler {

    @Override
    public ActionResult execute(ActionContext context) {
        Map<String, Object> config = context.configuration();
        
        String value = String.valueOf(config.get("value"));
        String algorithmName = config.getOrDefault("algorithm", "SHA256withRSA").toString();
        String privateKeyStr = String.valueOf(config.get("privateKey"));
        
        try {
            String pk = privateKeyStr
                    .replace("-----BEGIN PRIVATE KEY-----", "")
                    .replace("-----END PRIVATE KEY-----", "")
                    .replaceAll("\\s+", "");
            
            byte[] pkBytes = Base64.getDecoder().decode(pk);
            PKCS8EncodedKeySpec keySpec = new PKCS8EncodedKeySpec(pkBytes);
            KeyFactory keyFactory = KeyFactory.getInstance("RSA");
            PrivateKey privateKey = keyFactory.generatePrivate(keySpec);
            
            Signature signature = Signature.getInstance(algorithmName);
            signature.initSign(privateKey);
            signature.update(value.getBytes());
            
            byte[] signedBytes = signature.sign();
            String signedValue = Base64.getEncoder().encodeToString(signedBytes);
            
            return ActionResult.success(Map.of("data", signedValue));
        } catch (Exception e) {
            return ActionResult.failure("Failed to sign value: " + e.getMessage());
        }
    }
}
