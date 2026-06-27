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
        return new App("coingecko", "CoinGecko", """
                CoinGecko is the world's largest independent cryptocurrency data aggregator. The Crescendo CoinGecko app allows you to pull real-time crypto prices, volumes, and market data into your workflows.

                **What you can do with CoinGecko in Crescendo:**
                - Track Bitcoin prices every hour and alert a Telegram group if it drops below a threshold
                - Generate a daily summary of the top 10 trending coins and post it to a Slack channel
                - Sync the current Ethereum price into a Google Sheet for portfolio tracking
                - Trigger a buy/sell alert based on real-time market cap changes

                **Actions available:**
                - Get Price — fetch the current price of one or more cryptocurrencies in your preferred fiat currency
                - Get Coin Details — retrieve advanced metrics like trading volume and market cap

                **Who should use this:** Crypto traders, financial analysts, and fintech developers building portfolio trackers.

                **Authentication:** None required for public endpoints (API Key available for Pro limits).
                """,
                "https://www.google.com/s2/favicons?domain=coingecko.com&sz=128", AuthType.NONE,
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
