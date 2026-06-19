package com.crescendo.apps.coingecko;

import com.crescendo.execution.action.ActionContext;
import com.crescendo.execution.action.ActionMapping;
import com.crescendo.execution.action.ActionResult;
import tools.jackson.databind.ObjectMapper;

@ActionMapping(appKey = "coingecko", actionKey = "search")
public class CoinGeckoSearchHandler extends CoinGeckoHandler {

    public CoinGeckoSearchHandler(ObjectMapper objectMapper) {
        super(objectMapper);
    }

    @Override
public ActionResult execute(ActionContext context) {
        String query = required(context, "query");
        if (query.isBlank()) return ActionResult.failure("CoinGecko query is required");
        return get("/search?query={query}", query);
    }
}
