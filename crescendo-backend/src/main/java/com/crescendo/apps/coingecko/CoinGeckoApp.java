package com.crescendo.apps.coingecko;

import com.crescendo.app.App;
import com.crescendo.apps.AppDefinition;
import com.crescendo.enums.AuthType;
import org.springframework.stereotype.Component;

import java.util.List;
import java.util.Map;

@Component
public class CoinGeckoApp implements AppDefinition {

    @Override
    public App toApp() {
        return new App("coingecko", "CoinGecko", "Fetch cryptocurrency prices and market data",
                "/icons/coingecko.svg", AuthType.NONE,
                List.of(),
                List.of(
                        Map.of("actionKey", "simple-price", "name", "Get Simple Price",
                                "description", "Get current prices for one or more coins",
                                "configSchema", List.of(
                                        Map.of("key", "ids", "label", "Coin IDs", "type", "text", "required", true,
                                                "placeholder", "bitcoin,ethereum"),
                                        Map.of("key", "vsCurrencies", "label", "Currencies", "type", "text", "required", false,
                                                "placeholder", "usd,inr"))),
                        Map.of("actionKey", "coin-market", "name", "Get Coin Market",
                                "description", "Get market data for a coin",
                                "configSchema", List.of(
                                        Map.of("key", "id", "label", "Coin ID", "type", "text", "required", true,
                                                "placeholder", "bitcoin"),
                                        Map.of("key", "currency", "label", "Currency", "type", "text", "required", false,
                                                "placeholder", "usd"))),
                        Map.of("actionKey", "search", "name", "Search Coins",
                                "description", "Search CoinGecko coins by text",
                                "configSchema", List.of(
                                        Map.of("key", "query", "label", "Query", "type", "text", "required", true,
                                                "placeholder", "bitcoin")))
                )
        ).credentialSchema(List.of()).category("data").helpUrl("https://www.coingecko.com/en/api");
    }
}
