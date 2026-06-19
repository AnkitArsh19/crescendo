package com.crescendo.apps.coingecko;

import com.crescendo.execution.action.ActionContext;
import com.crescendo.execution.action.ActionMapping;
import com.crescendo.execution.action.ActionResult;
import tools.jackson.databind.ObjectMapper;

@ActionMapping(appKey = "coingecko", actionKey = "simple-price")
public class CoinGeckoSimplePriceHandler extends CoinGeckoHandler {

    public CoinGeckoSimplePriceHandler(ObjectMapper objectMapper) {
        super(objectMapper);
    }

    @Override
public ActionResult execute(ActionContext context) {
        String ids = required(context, "ids");
        if (ids.isBlank()) return ActionResult.failure("CoinGecko ids is required");
        String vsCurrencies = required(context, "vsCurrencies");
        if (vsCurrencies.isBlank()) vsCurrencies = "usd";
        return get("/simple/price?ids={ids}&vs_currencies={vsCurrencies}", ids, vsCurrencies);
    }
}
