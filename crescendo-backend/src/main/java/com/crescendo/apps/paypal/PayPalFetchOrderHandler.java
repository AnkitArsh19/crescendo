package com.crescendo.apps.paypal;

import com.crescendo.execution.action.ActionContext;
import com.crescendo.execution.action.ActionHandler;
import com.crescendo.execution.action.ActionMapping;
import com.crescendo.execution.action.ActionResult;
import org.springframework.http.HttpHeaders;
import org.springframework.stereotype.Component;
import org.springframework.web.client.RestClient;

import java.util.Map;

@Component
@ActionMapping(appKey = "paypal", actionKey = "fetch-order")
public class PayPalFetchOrderHandler implements ActionHandler {
    @Override
public ActionResult execute(ActionContext context) {
        Object orderId = context.configuration().get("orderId");
        if (orderId == null) return ActionResult.failure("Fetch Order requires orderId");
        try {
            String token = PayPalClient.accessToken(context.credentials());
            String response = RestClient.builder().baseUrl(PayPalClient.baseUrl(context.credentials())).build()
                    .get()
                    .uri("/v2/checkout/orders/{id}", orderId)
                    .header(HttpHeaders.AUTHORIZATION, "Bearer " + token)
                    .retrieve()
                    .body(String.class);
            return ActionResult.success(Map.of("body", response));
        } catch (Exception e) {
            return ActionResult.failure("PayPal fetch order failed: " + e.getMessage());
        }
    }
}
