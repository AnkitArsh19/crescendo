package com.crescendo.apps.totp;

import com.crescendo.execution.action.ActionContext;
import com.crescendo.execution.action.ActionHandler;
import com.crescendo.execution.action.ActionMapping;
import com.crescendo.execution.action.ActionResult;
import org.springframework.stereotype.Component;

import java.time.Instant;
import java.util.Map;

@Component
@ActionMapping(appKey = "totp", actionKey = "verify")
public class TotpVerifyHandler implements ActionHandler {
    @Override
public ActionResult execute(ActionContext context) {
        try {
            String secret = String.valueOf(context.configuration().get("secret"));
            String code = String.valueOf(context.configuration().get("code"));
            int digits = intValue(context.configuration().get("digits"), 6);
            int period = intValue(context.configuration().get("period"), 30);
            long counter = Instant.now().getEpochSecond() / period;
            boolean valid = false;
            for (long offset = -1; offset <= 1; offset++) {
                if (TotpHandler.code(secret, counter + offset, digits).equals(code)) {
                    valid = true;
                    break;
                }
            }
            return ActionResult.success(Map.of("valid", valid));
        } catch (Exception e) {
            return ActionResult.failure("Failed to verify TOTP: " + e.getMessage());
        }
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
