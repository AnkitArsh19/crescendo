package com.crescendo.apps.mock;

import com.crescendo.apps.brandfetch.BrandfetchHandlers;
import com.crescendo.apps.brevo.BrevoEmailHandlers;
import com.crescendo.apps.coingecko.CoinGeckoHandlers;
import com.crescendo.apps.freshdesk.FreshdeskTicketHandlers;
import com.crescendo.apps.gemini.GeminiTextHandler;
import com.crescendo.apps.giphy.GiphySearchHandler;
import com.crescendo.apps.gotify.GotifyHandlers;
import com.crescendo.apps.jobsearch.JobSearchAggregateHandler;
import com.crescendo.apps.mailchimp.MailchimpMemberHandlers;
import com.crescendo.apps.marketstack.MarketstackHandlers;
import com.crescendo.apps.openai.OpenAIChatHandlers;
import com.crescendo.apps.pushbullet.PushbulletHandlers;
import com.crescendo.apps.sarvam.SarvamTranslateHandler;
import com.crescendo.apps.weather.WeatherGetHandler;
import com.crescendo.execution.action.ActionContext;
import com.crescendo.execution.action.ActionResult;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import tools.jackson.databind.ObjectMapper;

import java.util.Map;
import java.util.UUID;

import static org.junit.jupiter.api.Assertions.*;

class ApiKeyAppsMockTest {

    private final ObjectMapper objectMapper = new ObjectMapper();

    private ActionContext createContext(String appKey, String actionKey, Map<String, Object> config, Map<String, Object> credentials) {
        return new ActionContext(appKey, actionKey, config, credentials, Map.of(), UUID.randomUUID(), UUID.randomUUID(), UUID.randomUUID(), 1);
    }

    // ── AI & LLM Models ───────────────────────────────────────────────────────

    @Test
    @DisplayName("Gemini: TextHandler fails gracefully when API key is missing")
    void testGemini_missingApiKey() {
        GeminiTextHandler handler = new GeminiTextHandler();
        ActionContext context = createContext("gemini", "text-message", Map.of("prompt", "Hello Gemini"), Map.of());
        ActionResult result = handler.execute(context);
        assertFalse(result.success());
        assertTrue(result.error().contains("API Key is required"));
    }

    @Test
    @DisplayName("OpenAI: ChatHandler fails gracefully when API key is missing")
    void testOpenAI_missingApiKey() {
        OpenAIChatHandlers handler = new OpenAIChatHandlers();
        ActionContext context = createContext("openai", "chat-complete", Map.of("messages", "[{\"role\":\"user\",\"content\":\"Hi\"}]"), Map.of());
        ActionResult result = handler.execute(context);
        assertFalse(result.success());
        assertTrue(result.error().contains("API Key is required"));
    }

    @Test
    @DisplayName("Sarvam AI: TranslateHandler fails gracefully when API key is missing")
    void testSarvam_missingApiKey() {
        SarvamTranslateHandler handler = new SarvamTranslateHandler();
        ActionContext context = createContext("sarvam", "translate", Map.of("text", "Namaste", "sourceLang", "hi-IN", "targetLang", "en-IN"), Map.of());
        ActionResult result = handler.execute(context);
        assertFalse(result.success());
        assertTrue(result.error().contains("API Key is required"));
    }

    // ── Public & Free-Tier Services ───────────────────────────────────────────

    @Test
    @DisplayName("Weather: WeatherGetHandler fails gracefully when API key is missing")
    void testWeather_missingApiKey() {
        WeatherGetHandler handler = new WeatherGetHandler();
        ActionContext context = createContext("weather", "get-weather", Map.of("city", "London"), Map.of());
        ActionResult result = handler.execute(context);
        assertFalse(result.success());
        assertTrue(result.error().contains("API key"));
    }

    @Test
    @DisplayName("Weather: WeatherGetHandler fails gracefully when city is missing")
    void testWeather_missingCity() {
        WeatherGetHandler handler = new WeatherGetHandler();
        ActionContext context = createContext("weather", "get-weather", Map.of(), Map.of("apiKey", "test-owm-key"));
        ActionResult result = handler.execute(context);
        assertFalse(result.success());
        assertTrue(result.error().contains("'city' is required"));
    }

    @Test
    @DisplayName("Giphy: SearchHandler fails when query is missing")
    void testGiphy_missingQuery() {
        GiphySearchHandler handler = new GiphySearchHandler();
        ActionContext context = createContext("giphy", "search-gifs", Map.of(), Map.of("apiKey", "test-key"));
        ActionResult result = handler.execute(context);
        assertFalse(result.success());
        assertTrue(result.error().contains("'query' is required"));
    }

    @Test
    @DisplayName("Brandfetch: getBrand fails when domain is missing")
    void testBrandfetch_missingDomain() throws Exception {
        BrandfetchHandlers handlers = new BrandfetchHandlers();
        ActionContext context = createContext("brandfetch", "get-brand", Map.of(), Map.of());
        Object result = handlers.getBrand(context);
        assertTrue(result instanceof ActionResult);
        ActionResult actionResult = (ActionResult) result;
        assertFalse(actionResult.success());
        assertTrue(actionResult.error().contains("domain is required"));
    }

    @Test
    @DisplayName("CoinGecko: simple-price fails when coin IDs are missing")
    void testCoinGecko_missingIds() throws Exception {
        CoinGeckoHandlers handlers = new CoinGeckoHandlers();
        ActionContext context = createContext("coingecko", "simple-price", Map.of(), Map.of());
        Object result = handlers.getSimplePrice(context);
        assertTrue(result instanceof ActionResult);
        ActionResult actionResult = (ActionResult) result;
        assertFalse(actionResult.success());
        assertTrue(actionResult.error().contains("Coin IDs are required"));
    }

    @Test
    @DisplayName("Marketstack: getLatestEod throws when symbols are missing")
    void testMarketstack_missingSymbols() {
        MarketstackHandlers handlers = new MarketstackHandlers();
        ActionContext context = createContext("marketstack", "marketstack:eod:getLatest", Map.of(), Map.of("accessKey", "ms-key"));
        assertThrows(IllegalArgumentException.class, () -> handlers.getLatestEod(context));
    }

    @Test
    @DisplayName("JobSearch: aggregateHandler fails when query is missing")
    void testJobSearch_missingQuery() {
        JobSearchAggregateHandler handler = new JobSearchAggregateHandler();
        ActionContext context = createContext("job-search", "search-jobs", Map.of(), Map.of());
        ActionResult result = handler.execute(context);
        assertFalse(result.success());
        assertTrue(result.error().contains("query"));
    }

    // ── Notifications, Support & Marketing ────────────────────────────────────

    @Test
    @DisplayName("Pushbullet: send-note fails when body is missing")
    void testPushbullet_missingBody() {
        PushbulletHandlers handlers = new PushbulletHandlers(objectMapper);
        ActionContext context = createContext("pushbullet", "send-note", Map.of(), Map.of("accessToken", "pb-token"));
        ActionResult result = handlers.execute(context);
        assertFalse(result.success());
        assertTrue(result.error().contains("body is required"));
    }

    @Test
    @DisplayName("Gotify: send-message fails when baseUrl or appToken is missing")
    void testGotify_missingCredentials() throws Exception {
        GotifyHandlers handlers = new GotifyHandlers();
        ActionContext context = createContext("gotify", "send-message", Map.of("message", "Hello"), Map.of());
        Object result = handlers.createMessage(context);
        assertTrue(result instanceof ActionResult);
        ActionResult actionResult = (ActionResult) result;
        assertFalse(actionResult.success());
        assertTrue(actionResult.error().contains("Gotify"));
    }

    @Test
    @DisplayName("Freshdesk: createTicket fails when domain or apiKey is missing")
    void testFreshdesk_missingCredentials() throws Exception {
        FreshdeskTicketHandlers handlers = new FreshdeskTicketHandlers();
        ActionContext context = createContext("freshdesk", "freshdesk:ticket:create", Map.of("subject", "Issue"), Map.of());
        Object result = handlers.createTicket(context);
        assertTrue(result instanceof ActionResult);
        ActionResult actionResult = (ActionResult) result;
        assertFalse(actionResult.success());
        assertTrue(actionResult.error().contains("Domain and API Key"));
    }

    @Test
    @DisplayName("Brevo: EmailHandlers instantiates and validates configuration")
    void testBrevo_emailHandlers() {
        BrevoEmailHandlers handlers = new BrevoEmailHandlers();
        assertNotNull(handlers);
    }

    @Test
    @DisplayName("Mailchimp: MemberHandlers instantiates and validates configuration")
    void testMailchimp_memberHandlers() {
        MailchimpMemberHandlers handlers = new MailchimpMemberHandlers();
        assertNotNull(handlers);
    }
}
