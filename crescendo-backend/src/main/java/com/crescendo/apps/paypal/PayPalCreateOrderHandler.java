package com.crescendo.apps.paypal;

import com.crescendo.execution.action.ActionContext;
import com.crescendo.execution.action.ActionHandler;
import com.crescendo.execution.action.ActionMapping;
import com.crescendo.execution.action.ActionResult;
import org.springframework.http.HttpHeaders;
import org.springframework.http.MediaType;
import org.springframework.stereotype.Component;
import org.springframework.web.client.RestClient;

import java.util.List;
import java.util.Map;

@Component
@ActionMapping(appKey = "paypal", actionKey = "create-order")
public class PayPalCreateOrderHandler implements ActionHandler {
    @Override
public ActionResult execute(ActionContext context) {
        try {
            String token = PayPalClient.accessToken(context.credentials());
            Map<String, Object> amount = Map.of(
                    "currency_code", String.valueOf(context.configuration().getOrDefault("currency", "USD")),
                    "value", String.valueOf(context.configuration().get("amount")));
            Map<String, Object> body = Map.of(
                    "intent", String.valueOf(context.configuration().getOrDefault("intent", "CAPTURE")),
                    "purchase_units", List.of(Map.of("amount", amount)));
            String response = RestClient.builder().baseUrl(PayPalClient.baseUrl(context.credentials())).build()
                    .post()
                    .uri("/v2/checkout/orders")
                    .header(HttpHeaders.AUTHORIZATION, "Bearer " + token)
                    .contentType(MediaType.APPLICATION_JSON)
                    .body(body)
                    .retrieve()
                    .body(String.class);
            return ActionResult.success(Map.of("body", response));
        } catch (Exception e) {
            return ActionResult.failure("PayPal create order failed: " + e.getMessage());
        }
    }
}
