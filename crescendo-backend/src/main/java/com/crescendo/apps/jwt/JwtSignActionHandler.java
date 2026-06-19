package com.crescendo.apps.jwt;

import com.crescendo.execution.action.ActionContext;
import com.crescendo.execution.action.ActionHandler;
import com.crescendo.execution.action.ActionMapping;
import com.crescendo.execution.action.ActionResult;
import io.jsonwebtoken.Jwts;
import io.jsonwebtoken.security.Keys;
import org.springframework.stereotype.Component;

import javax.crypto.SecretKey;
import java.nio.charset.StandardCharsets;
import java.time.Instant;
import java.util.Date;
import java.util.Map;

@Component
@ActionMapping(appKey = "jwt", actionKey = "sign")
@SuppressWarnings("unchecked")
public class JwtSignActionHandler implements ActionHandler {

    @Override
public ActionResult execute(ActionContext context) {
        Map<String, Object> config = context.configuration();
        if (config == null || !config.containsKey("secret") || !config.containsKey("payload")) {
            return ActionResult.failure("Sign action requires 'secret' and 'payload' in configuration");
        }

        String secretStr = String.valueOf(config.get("secret"));
        Object payloadObj = config.get("payload");

        if (!(payloadObj instanceof Map)) {
            return ActionResult.failure("Payload must be a valid JSON object");
        }

        Map<String, Object> payload = (Map<String, Object>) payloadObj;

        try {
            byte[] keyBytes = secretStr.getBytes(StandardCharsets.UTF_8);
            if (keyBytes.length < 32) {
                return ActionResult.failure("HS256 secret must be at least 32 bytes");
            }
            SecretKey key = Keys.hmacShaKeyFor(keyBytes);

            var builder = Jwts.builder()
                    .claims(payload)
                    .signWith(key);

            if (config.containsKey("expiresIn") && config.get("expiresIn") != null) {
                try {
                    long expiresInSeconds = Long.parseLong(String.valueOf(config.get("expiresIn")));
                    Instant expiration = Instant.now().plusSeconds(expiresInSeconds);
                    builder.expiration(Date.from(expiration));
                } catch (NumberFormatException e) {
                    return ActionResult.failure("expiresIn must be a valid number");
                }
            }

            String token = builder.compact();
            return ActionResult.success(Map.of("token", token));

        } catch (Exception e) {
            return ActionResult.failure("Failed to sign JWT: " + e.getMessage());
        }
    }
}
