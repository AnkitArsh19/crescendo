package com.crescendo.apps.totp;

import com.crescendo.execution.action.ActionContext;
import com.crescendo.execution.action.ActionHandler;
import com.crescendo.execution.action.ActionMapping;
import com.crescendo.execution.action.ActionResult;
import org.springframework.stereotype.Component;

import javax.crypto.Mac;
import javax.crypto.spec.SecretKeySpec;
import java.nio.ByteBuffer;
import java.time.Instant;
import java.util.Map;

@Component
@ActionMapping(appKey = "totp", actionKey = "generate")
public class TotpHandler implements ActionHandler {
    @Override
public ActionResult execute(ActionContext context) {
        return generate(context);
    }

    ActionResult generate(ActionContext context) {
        try {
            String secret = String.valueOf(context.configuration().get("secret"));
            int digits = intValue(context.configuration().get("digits"), 6);
            int period = intValue(context.configuration().get("period"), 30);
            long counter = Instant.now().getEpochSecond() / period;
            String code = code(secret, counter, digits);
            return ActionResult.success(Map.of("code", code, "period", period, "digits", digits));
        } catch (Exception e) {
            return ActionResult.failure("Failed to generate TOTP: " + e.getMessage());
        }
    }

    static String code(String base32Secret, long counter, int digits) throws Exception {
        byte[] key = base32Decode(base32Secret);
        byte[] message = ByteBuffer.allocate(8).putLong(counter).array();
        Mac mac = Mac.getInstance("HmacSHA1");
        mac.init(new SecretKeySpec(key, "HmacSHA1"));
        byte[] hash = mac.doFinal(message);
        int offset = hash[hash.length - 1] & 0x0f;
        int binary = ((hash[offset] & 0x7f) << 24)
                | ((hash[offset + 1] & 0xff) << 16)
                | ((hash[offset + 2] & 0xff) << 8)
                | (hash[offset + 3] & 0xff);
        int otp = binary % (int) Math.pow(10, digits);
        return String.format("%0" + digits + "d", otp);
    }

    static byte[] base32Decode(String value) {
        String alphabet = "ABCDEFGHIJKLMNOPQRSTUVWXYZ234567";
        String cleaned = value.replace("=", "").replace(" ", "").toUpperCase();
        java.io.ByteArrayOutputStream out = new java.io.ByteArrayOutputStream();
        int buffer = 0;
        int bitsLeft = 0;
        for (char c : cleaned.toCharArray()) {
            int val = alphabet.indexOf(c);
            if (val < 0) continue;
            buffer = (buffer << 5) | val;
            bitsLeft += 5;
            if (bitsLeft >= 8) {
                out.write((buffer >> (bitsLeft - 8)) & 0xff);
                bitsLeft -= 8;
            }
        }
        return out.toByteArray();
    }

    int intValue(Object value, int fallback) {
        if (value instanceof Number n) return n.intValue();
        try {
            return value == null ? fallback : Integer.parseInt(String.valueOf(value));
        } catch (NumberFormatException e) {
            return fallback;
        }
    }
}
