package com.crescendo.apps.simpleapi;

import com.crescendo.execution.action.ActionContext;
import com.crescendo.execution.action.ActionHandler;
import com.crescendo.execution.action.ActionMapping;
import com.crescendo.execution.action.ActionResult;
import com.crescendo.utils.RestClient;
import org.springframework.stereotype.Component;
import org.springframework.web.util.UriComponentsBuilder;
import java.util.Map;


@Component
@ActionMapping(appKey = "simpleapi", actionKey = "execute")
public class SimpleApiActionHandler implements ActionHandler {

    @Override
    public ActionResult execute(ActionContext context) {
        Map<String, Object> config = context.configuration();
        String apiToken = context.credentials() != null ? String.valueOf(context.credentials().get("apiToken")) : null;
        if (apiToken == null || apiToken.isBlank()) {
            return ActionResult.failure("API Token is required");
        }

        String operation = String.valueOf(config.get("operation"));
        String endpoint = "https://onesimpleapi.com/api/";
        UriComponentsBuilder builder;

        switch (operation) {
            case "pdf":
            case "seo":
            case "screenshot":
            case "qrCode":
            case "expandURL":
                String opName = operation.equals("qrCode") ? "qr" : operation.equals("expandURL") ? "expand" : operation;
                builder = UriComponentsBuilder.fromUriString(endpoint + opName)
                        .queryParam("token", apiToken)
                        .queryParam("url", config.get("url"));
                break;
            case "instagramProfile":
            case "spotifyProfile":
                String op = operation.equals("instagramProfile") ? "instagram" : "spotify";
                builder = UriComponentsBuilder.fromUriString(endpoint + op)
                        .queryParam("token", apiToken)
                        .queryParam("username", config.get("username"));
                break;
            case "exchangeRate":
                builder = UriComponentsBuilder.fromUriString(endpoint + "exchange")
                        .queryParam("token", apiToken)
                        .queryParam("from", config.get("baseCurrency"))
                        .queryParam("to", config.get("targetCurrency"));
                break;
            case "imageMetadata":
                builder = UriComponentsBuilder.fromUriString(endpoint + "image_metadata")
                        .queryParam("token", apiToken)
                        .queryParam("url", config.get("url"));
                break;
            default:
                return ActionResult.failure("Unknown operation: " + operation);
        }

        builder.queryParam("output", "json");

        try {
            Object result = RestClient.builder()
                    .url(builder.build().toUriString())
                    .get()
                    .execute();

            return ActionResult.success(Map.of("data", result));
        } catch (Exception e) {
            return ActionResult.failure("One Simple API request failed: " + e.getMessage());
        }
    }
}
