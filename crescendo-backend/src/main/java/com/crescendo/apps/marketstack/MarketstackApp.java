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
        return new App("marketstack", "Marketstack", """
                Marketstack provides an easy-to-use REST API offering global stock market data. The Crescendo Marketstack app allows you to retrieve real-time and historical stock data effortlessly.

                **What you can do with Marketstack in Crescendo:**
                - Check the closing price of specific stocks daily and append them to a Google Sheet
                - Send an SMS via Twilio if a monitored stock drops by more than 5%
                - Generate a weekly financial summary report for your personal Slack channel
                - Enrich internal dashboards with up-to-date market capitalization figures

                **Actions available:**
                - Get End of Day Data — fetch closing prices and volumes for specific ticker symbols

                **Who should use this:** Financial analysts, day traders, and developers building automated portfolio trackers.

                **Authentication:** API Key (create one on the Marketstack dashboard).
                """,
                "https://www.google.com/s2/favicons?domain=marketstack.com&sz=128", AuthType.NONE,
                List.of(),
                List.of(
                        Map.of("actionKey", "marketstack:eod:getLatest", "name", "Latest EOD",
                                "description", "Fetch latest end-of-day prices for stock symbols",
                                "configSchema", List.of(
                                        Map.of("key", "symbols", "label", "Symbols", "type", "text", "required", true,
                                                "placeholder", "AAPL,MSFT"),
                                        Map.of("key", "limit", "label", "Limit", "type", "text", "required", false,
                                                "placeholder", "10")))
                )
        ).credentialSchema(List.of()).category("data").helpUrl("https://marketstack.com/documentation");
    }
}
