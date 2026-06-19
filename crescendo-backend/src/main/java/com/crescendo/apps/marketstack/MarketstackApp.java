package com.crescendo.apps.marketstack;

import com.crescendo.app.App;
import com.crescendo.apps.AppDefinition;
import com.crescendo.enums.AuthType;
import org.springframework.stereotype.Component;

import java.util.List;
import java.util.Map;

@Component
public class MarketstackApp implements AppDefinition {

    @Override
    public App toApp() {
        return new App("marketstack", "Marketstack", "Fetch stock market end-of-day data",
                "/icons/marketstack.svg", AuthType.APIKEY,
                List.of(),
                List.of(
                        Map.of("actionKey", "latest-eod", "name", "Latest EOD",
                                "description", "Fetch latest end-of-day prices for stock symbols",
                                "configSchema", List.of(
                                        Map.of("key", "symbols", "label", "Symbols", "type", "text", "required", true,
                                                "placeholder", "AAPL,MSFT"),
                                        Map.of("key", "limit", "label", "Limit", "type", "text", "required", false,
                                                "placeholder", "10")))
                )
        ).credentialSchema(List.of(
                Map.of("key", "accessKey", "label", "Access Key", "type", "password", "required", true)
        )).category("data").helpUrl("https://marketstack.com/documentation");
    }
}
