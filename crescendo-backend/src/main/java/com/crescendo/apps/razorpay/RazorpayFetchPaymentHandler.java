package com.crescendo.apps.razorpay;

import com.crescendo.execution.action.ActionContext;
import com.crescendo.execution.action.ActionHandler;
import com.crescendo.execution.action.ActionMapping;
import com.crescendo.execution.action.ActionResult;
import org.springframework.http.HttpHeaders;
import org.springframework.http.MediaType;
import org.springframework.stereotype.Component;
import org.springframework.web.client.RestClient;

import java.util.Map;

@Component
@ActionMapping(appKey = "razorpay", actionKey = "fetch-payment")
public class RazorpayFetchPaymentHandler implements ActionHandler {
    private final RestClient restClient = RestClient.builder()
            .baseUrl("https://api.razorpay.com/v1")
            .defaultHeader(HttpHeaders.ACCEPT, MediaType.APPLICATION_JSON_VALUE)
            .build();

    @Override
public ActionResult execute(ActionContext context) {
        Object paymentId = context.configuration().get("paymentId");
        if (paymentId == null) return ActionResult.failure("Fetch Payment requires paymentId");
        try {
            String response = restClient.get()
                    .uri("/payments/{id}", paymentId)
                    .header(HttpHeaders.AUTHORIZATION, RazorpayActionHandler.basic(context.credentials()))
                    .retrieve()
                    .body(String.class);
            return ActionResult.success(Map.of("body", response));
        } catch (Exception e) {
            return ActionResult.failure("Razorpay fetch payment failed: " + e.getMessage());
        }
    }
}
