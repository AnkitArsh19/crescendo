package com.crescendo.apps.razorpay;

import com.crescendo.execution.action.ActionContext;
import com.crescendo.execution.action.ActionHandler;
import com.crescendo.execution.action.ActionMapping;
import com.crescendo.execution.action.ActionResult;
import org.springframework.stereotype.Component;

import java.util.HashMap;
import java.util.Map;

@Component
@ActionMapping(appKey = "razorpay", actionKey = "create-payment-link")
public class RazorpayCreatePaymentLinkHandler implements ActionHandler {
    private final RazorpayActionHandler delegate = new RazorpayActionHandler();

    @Override
public ActionResult execute(ActionContext context) {
        Map<String, Object> body = new HashMap<>();
        body.put("amount", context.configuration().get("amount"));
        body.put("currency", context.configuration().getOrDefault("currency", "INR"));
        if (context.configuration().get("description") != null) body.put("description", context.configuration().get("description"));
        if (context.configuration().get("customer") != null) body.put("customer", context.configuration().get("customer"));
        return delegate.post(context, "/payment_links", body);
    }
}
