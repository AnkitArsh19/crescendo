package com.crescendo.apps.paypal;

import com.crescendo.execution.action.ActionContext;
import com.crescendo.execution.action.ActionMapping;
import com.crescendo.utils.RestClient;
import org.springframework.stereotype.Component;

import java.util.List;
import java.util.Map;

@Component
public class PayPalOrderHandlers {

    @ActionMapping(appKey = "paypal", actionKey = "paypal:order:create")
    public Object createOrder(ActionContext context) throws Exception {
        String token = PayPalSupport.accessToken(context.credentials());
        
        Map<String, Object> amount = Map.of(
                "currency_code", String.valueOf(context.configuration().getOrDefault("currency", "USD")),
                "value", String.valueOf(context.configuration().get("amount")));
        
        Map<String, Object> body = Map.of(
                "intent", String.valueOf(context.configuration().getOrDefault("intent", "CAPTURE")),
                "purchase_units", List.of(Map.of("amount", amount)));
                
        return RestClient.builder()
                .url(PayPalSupport.baseUrl(context.credentials()) + "/v2/checkout/orders")
                .header("Authorization", "Bearer " + token)
                .header("Content-Type", "application/json")
                .post(body)
                .execute();
    }

    @ActionMapping(appKey = "paypal", actionKey = "paypal:order:get")
    public Object getOrder(ActionContext context) throws Exception {
        String token = PayPalSupport.accessToken(context.credentials());
        String orderId = context.getString("orderId");
        
        return RestClient.builder()
                .url(PayPalSupport.baseUrl(context.credentials()) + "/v2/checkout/orders/" + orderId)
                .header("Authorization", "Bearer " + token)
                .get()
                .execute();
    }
}
