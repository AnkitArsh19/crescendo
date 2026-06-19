package com.crescendo.apps.jwt;

import com.crescendo.execution.action.ActionContext;
import com.crescendo.execution.action.ActionHandler;
import com.crescendo.execution.action.ActionMapping;
import com.crescendo.execution.action.ActionResult;
import io.jsonwebtoken.Claims;
import io.jsonwebtoken.Jws;
import io.jsonwebtoken.Jwts;
import io.jsonwebtoken.security.Keys;
import org.springframework.stereotype.Component;

import javax.crypto.SecretKey;
import java.nio.charset.StandardCharsets;
import java.util.Map;

@Component
@ActionMapping(appKey = "jwt", actionKey = "verify")
public class JwtVerifyActionHandler implements ActionHandler {

    @Override
public ActionResult execute(ActionContext context) {
        Map<String, Object> config = context.configuration();
        if (config == null || !config.containsKey("secret") || !config.containsKey("token")) {
            return ActionResult.failure("Verify action requires 'secret' and 'token' in configuration");
        }

        String secretStr = String.valueOf(config.get("secret"));
        String token = String.valueOf(config.get("token"));

        try {
            byte[] keyBytes = secretStr.getBytes(StandardCharsets.UTF_8);
            if (keyBytes.length < 32) {
                return ActionResult.failure("HS256 secret must be at least 32 bytes");
            }
            SecretKey key = Keys.hmacShaKeyFor(keyBytes);

            Jws<Claims> jws = Jwts.parser()
                    .verifyWith(key)
                    .build()
                    .parseSignedClaims(token);

            // JWS body is already a map of claims
            return ActionResult.success(Map.of(
                "isValid", true,
                "payload", jws.getPayload()
            ));

        } catch (Exception e) {
            return ActionResult.failure("Token verification failed: " + e.getMessage());
        }
    }
}
