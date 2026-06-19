package com.crescendo.apps.coingecko;

import com.crescendo.execution.action.ActionContext;
import com.crescendo.execution.action.ActionMapping;
import com.crescendo.execution.action.ActionResult;
import tools.jackson.databind.ObjectMapper;

@ActionMapping(appKey = "coingecko", actionKey = "coin-market")
public class CoinGeckoCoinMarketHandler extends CoinGeckoHandler {

    public CoinGeckoCoinMarketHandler(ObjectMapper objectMapper) {
        super(objectMapper);
    }

    @Override
public ActionResult execute(ActionContext context) {
        String id = required(context, "id");
        if (id.isBlank()) return ActionResult.failure("CoinGecko coin id is required");
        String currency = required(context, "currency");
        if (currency.isBlank()) currency = "usd";
        return get("/coins/markets?vs_currency={currency}&ids={id}", currency, id);
    }
}
