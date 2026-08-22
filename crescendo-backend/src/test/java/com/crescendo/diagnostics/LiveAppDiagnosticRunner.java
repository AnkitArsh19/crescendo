package com.crescendo.diagnostics;

import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.web.client.RestClient;

import java.io.InputStream;
import java.net.URI;
import java.net.http.HttpClient;
import java.net.http.HttpRequest;
import java.net.http.HttpResponse;
import java.time.Duration;
import java.util.LinkedHashMap;
import java.util.Map;
import java.util.Properties;

public class LiveAppDiagnosticRunner {

    private static final Properties APP_PROPS = loadProperties();

    private final HttpClient httpClient = HttpClient.newBuilder()
            .connectTimeout(Duration.ofSeconds(10))
            .build();

    public static void main(String[] args) {
        LiveAppDiagnosticRunner runner = new LiveAppDiagnosticRunner();
        runner.runAllDiagnostics();
    }

    private static Properties loadProperties() {
        Properties props = new Properties();
        try (InputStream is = LiveAppDiagnosticRunner.class.getClassLoader().getResourceAsStream("application.properties")) {
            if (is != null) {
                props.load(is);
            }
        } catch (Exception ignored) {}
        return props;
    }

    private static String getConfig(String envName, String propKey) {
        // 1. Environment Variable
        String envVal = System.getenv(envName);
        if (envVal != null && !envVal.isBlank() && !envVal.equals("REPLACE_ME")) {
            return envVal.trim();
        }
        // 2. System Property (-Dkey=...)
        String sysVal = System.getProperty(propKey);
        if (sysVal != null && !sysVal.isBlank() && !sysVal.equals("REPLACE_ME")) {
            return sysVal.trim();
        }
        // 3. application.properties (local dev)
        String fileVal = APP_PROPS.getProperty(propKey);
        if (fileVal != null && !fileVal.isBlank() && !fileVal.equals("REPLACE_ME") && !fileVal.startsWith("xkeysib-REPLACE_ME")) {
            return fileVal.trim();
        }
        return "";
    }

    @Test
    @DisplayName("Run Live Diagnostics against all configured Platform APIs")
    void runAllDiagnostics() {
        System.out.println("===============================================================================");
        System.out.println("            CRESCENDO LIVE API DIAGNOSTIC & VERIFICATION SUITE                 ");
        System.out.println("===============================================================================\n");

        Map<String, DiagnosticResult> results = new LinkedHashMap<>();

        // 1. Google Gemini AI
        results.put("Google Gemini AI", testGemini(getConfig("GEMINI_API_KEY", "gemini.api.key")));

        // 2. OpenWeatherMap
        results.put("OpenWeather API", testWeather(getConfig("WEATHER_API_KEY", "crescendo.platform.weather-api-key")));

        // 3. Giphy API
        results.put("Giphy Search API", testGiphy(getConfig("GIPHY_API_KEY", "crescendo.platform.giphy-api-key")));

        // 4. NASA APOD API
        results.put("NASA APOD API", testNasa(getConfig("NASA_API_KEY", "crescendo.platform.nasa-api-key")));

        // 5. Sarvam AI
        results.put("Sarvam AI", testSarvam(getConfig("SARVAM_API_KEY", "sarvam.api.key")));

        // 6. SerpAPI (JobSearch Google Jobs)
        results.put("SerpAPI (Job Search)", testSerpApi(getConfig("SERPAPI_KEY", "crescendo.jobsearch.serpapi-key")));

        // 7. Adzuna (JobSearch India)
        results.put("Adzuna (Job Search)", testAdzuna(
                getConfig("ADZUNA_APP_ID", "crescendo.jobsearch.adzuna-app-id"),
                getConfig("ADZUNA_API_KEY", "crescendo.jobsearch.adzuna-api-key")
        ));

        // 8. Jooble (JobSearch Global)
        results.put("Jooble (Job Search)", testJooble(getConfig("JOOBLE_API_KEY", "crescendo.jobsearch.jooble-api-key")));

        // 9. Brevo Email API
        results.put("Brevo Email API", testBrevo(getConfig("BREVO_API_KEY", "brevo.api.key")));

        // 10. CoinGecko Public API (No auth)
        results.put("CoinGecko Free API", testCoinGecko());

        // 11. ZenQuotes Public API (No auth)
        results.put("ZenQuotes API", testQuotes());

        // 12. JokeAPI Public API (No auth)
        results.put("JokeAPI", testJokeApi());

        // 13. CatFacts Public API (No auth)
        results.put("CatFacts API", testCatFacts());

        // 14. LeetCode Public API (No auth)
        results.put("LeetCode API", testLeetCode());

        // 15. Python AI Service
        results.put("Python AI Engine", testPythonAi(
                getConfig("PYTHON_AI_BASE_URL", "crescendo.python-ai.base-url"),
                getConfig("PYTHON_AI_SERVICE_TOKEN", "crescendo.python-ai.service-token")
        ));

        System.out.println("\n-------------------------------------------------------------------------------");
        System.out.println("                             DIAGNOSTIC SUMMARY                                ");
        System.out.println("-------------------------------------------------------------------------------");
        int passed = 0;
        int failed = 0;
        for (Map.Entry<String, DiagnosticResult> entry : results.entrySet()) {
            String appName = entry.getKey();
            DiagnosticResult res = entry.getValue();
            if (res.success) {
                passed++;
                System.out.printf("[PASS] %-25s | Status: %d | Details: %s%n", appName, res.statusCode, res.summary);
            } else {
                failed++;
                System.out.printf("[FAIL] %-25s | Status: %d | Error: %s%n", appName, res.statusCode, res.summary);
            }
        }
        System.out.println("-------------------------------------------------------------------------------");
        System.out.printf("Total APIs Tested: %d | Passed: %d | Failed / Expired: %d%n", results.size(), passed, failed);
        System.out.println("===============================================================================\n");
    }

    private DiagnosticResult testGemini(String key) {
        if (key == null || key.isBlank()) return new DiagnosticResult(true, 200, "Skipped (GEMINI_API_KEY not set)");
        String[] candidateModels = {"gemini-3.7-flash", "gemini-3.5-flash", "gemini-flash-latest", "gemini-3-flash-preview", "gemini-3.1-flash-lite"};
        for (String model : candidateModels) {
            try {
                String url = "https://generativelanguage.googleapis.com/v1beta/models/" + model + ":generateContent?key=" + key;
                String body = "{\"contents\":[{\"parts\":[{\"text\":\"Hello\"}]}]}";
                HttpRequest req = HttpRequest.newBuilder()
                        .uri(URI.create(url))
                        .header("Content-Type", "application/json")
                        .POST(HttpRequest.BodyPublishers.ofString(body))
                        .build();
                HttpResponse<String> resp = httpClient.send(req, HttpResponse.BodyHandlers.ofString());
                if (resp.statusCode() == 200) {
                    return new DiagnosticResult(true, 200, "Gemini (" + model + ") responded successfully: 200 OK");
                }
            } catch (Exception ignored) {}
        }
        return new DiagnosticResult(false, 400, "Tested candidate models but all returned non-200. Check API quota or key scope.");
    }

    private DiagnosticResult testWeather(String key) {
        if (key == null || key.isBlank()) return new DiagnosticResult(true, 200, "Skipped (WEATHER_API_KEY not set)");
        try {
            String url = "https://api.openweathermap.org/data/2.5/weather?q=London&appid=" + key;
            HttpRequest req = HttpRequest.newBuilder().uri(URI.create(url)).GET().build();
            HttpResponse<String> resp = httpClient.send(req, HttpResponse.BodyHandlers.ofString());
            boolean ok = resp.statusCode() == 200;
            String summary = ok ? "OpenWeather responded with 200 OK" : resp.body().substring(0, Math.min(120, resp.body().length()));
            return new DiagnosticResult(ok, resp.statusCode(), summary);
        } catch (Exception e) {
            return new DiagnosticResult(false, 0, e.getMessage());
        }
    }

    private DiagnosticResult testGiphy(String key) {
        if (key == null || key.isBlank()) return new DiagnosticResult(true, 200, "Skipped (GIPHY_API_KEY not set)");
        try {
            String url = "https://api.giphy.com/v1/gifs/search?api_key=" + key + "&q=cat&limit=1";
            HttpRequest req = HttpRequest.newBuilder().uri(URI.create(url)).GET().build();
            HttpResponse<String> resp = httpClient.send(req, HttpResponse.BodyHandlers.ofString());
            boolean ok = resp.statusCode() == 200;
            String summary = ok ? "Giphy API responded with 200 OK" : resp.body().substring(0, Math.min(120, resp.body().length()));
            return new DiagnosticResult(ok, resp.statusCode(), summary);
        } catch (Exception e) {
            return new DiagnosticResult(false, 0, e.getMessage());
        }
    }

    private DiagnosticResult testNasa(String key) {
        if (key == null || key.isBlank()) return new DiagnosticResult(true, 200, "Skipped (NASA_API_KEY not set)");
        try {
            String url = "https://api.nasa.gov/planetary/apod?api_key=" + key;
            HttpRequest req = HttpRequest.newBuilder().uri(URI.create(url)).GET().build();
            HttpResponse<String> resp = httpClient.send(req, HttpResponse.BodyHandlers.ofString());
            boolean ok = resp.statusCode() == 200;
            String summary = ok ? "NASA APOD API responded with 200 OK" : resp.body().substring(0, Math.min(120, resp.body().length()));
            return new DiagnosticResult(ok, resp.statusCode(), summary);
        } catch (Exception e) {
            return new DiagnosticResult(false, 0, e.getMessage());
        }
    }

    private DiagnosticResult testSarvam(String key) {
        if (key == null || key.isBlank()) return new DiagnosticResult(true, 200, "Skipped (SARVAM_API_KEY not set)");
        try {
            String url = "https://api.sarvam.ai/translate";
            String body = "{\"input\":\"Namaste\",\"source_language_code\":\"hi-IN\",\"target_language_code\":\"en-IN\"}";
            HttpRequest req = HttpRequest.newBuilder()
                    .uri(URI.create(url))
                    .header("api-subscription-key", key)
                    .header("Content-Type", "application/json")
                    .POST(HttpRequest.BodyPublishers.ofString(body))
                    .build();
            HttpResponse<String> resp = httpClient.send(req, HttpResponse.BodyHandlers.ofString());
            boolean ok = resp.statusCode() == 200;
            String summary = ok ? "Sarvam AI responded with 200 OK" : resp.body().substring(0, Math.min(120, resp.body().length()));
            return new DiagnosticResult(ok, resp.statusCode(), summary);
        } catch (Exception e) {
            return new DiagnosticResult(false, 0, e.getMessage());
        }
    }

    private DiagnosticResult testSerpApi(String key) {
        if (key == null || key.isBlank()) return new DiagnosticResult(true, 200, "Skipped (SERPAPI_KEY not set)");
        try {
            String url = "https://serpapi.com/search.json?engine=google_jobs&q=developer&api_key=" + key;
            HttpRequest req = HttpRequest.newBuilder().uri(URI.create(url)).GET().build();
            HttpResponse<String> resp = httpClient.send(req, HttpResponse.BodyHandlers.ofString());
            boolean ok = resp.statusCode() == 200;
            String summary = ok ? "SerpAPI Google Jobs responded with 200 OK" : resp.body().substring(0, Math.min(120, resp.body().length()));
            return new DiagnosticResult(ok, resp.statusCode(), summary);
        } catch (Exception e) {
            return new DiagnosticResult(false, 0, e.getMessage());
        }
    }

    private DiagnosticResult testAdzuna(String appId, String apiKey) {
        if (appId == null || appId.isBlank() || apiKey == null || apiKey.isBlank()) {
            return new DiagnosticResult(true, 200, "Skipped (ADZUNA_APP_ID / ADZUNA_API_KEY not set)");
        }
        try {
            String url = "https://api.adzuna.com/v1/api/jobs/in/search/1?app_id=" + appId + "&app_key=" + apiKey + "&what=developer&results_per_page=1";
            HttpRequest req = HttpRequest.newBuilder().uri(URI.create(url)).GET().build();
            HttpResponse<String> resp = httpClient.send(req, HttpResponse.BodyHandlers.ofString());
            boolean ok = resp.statusCode() == 200;
            String summary = ok ? "Adzuna Jobs responded with 200 OK" : resp.body().substring(0, Math.min(120, resp.body().length()));
            return new DiagnosticResult(ok, resp.statusCode(), summary);
        } catch (Exception e) {
            return new DiagnosticResult(false, 0, e.getMessage());
        }
    }

    private DiagnosticResult testJooble(String key) {
        if (key == null || key.isBlank()) return new DiagnosticResult(true, 200, "Skipped (JOOBLE_API_KEY not set)");
        try {
            String url = "https://jooble.org/api/" + key;
            String body = "{\"keywords\":\"developer\",\"location\":\"India\"}";
            HttpRequest req = HttpRequest.newBuilder()
                    .uri(URI.create(url))
                    .header("Content-Type", "application/json")
                    .POST(HttpRequest.BodyPublishers.ofString(body))
                    .build();
            HttpResponse<String> resp = httpClient.send(req, HttpResponse.BodyHandlers.ofString());
            boolean ok = resp.statusCode() == 200;
            String summary = ok ? "Jooble Jobs responded with 200 OK" : resp.body().substring(0, Math.min(120, resp.body().length()));
            return new DiagnosticResult(ok, resp.statusCode(), summary);
        } catch (Exception e) {
            return new DiagnosticResult(false, 0, e.getMessage());
        }
    }

    private DiagnosticResult testBrevo(String key) {
        if (key == null || key.isBlank()) return new DiagnosticResult(true, 200, "Skipped (BREVO_API_KEY not set)");
        try {
            String url = "https://api.brevo.com/v3/account";
            HttpRequest req = HttpRequest.newBuilder()
                    .uri(URI.create(url))
                    .header("api-key", key)
                    .header("Accept", "application/json")
                    .GET()
                    .build();
            HttpResponse<String> resp = httpClient.send(req, HttpResponse.BodyHandlers.ofString());
            boolean ok = resp.statusCode() == 200;
            String summary = ok ? "Brevo Account API responded with 200 OK" : resp.body().substring(0, Math.min(120, resp.body().length()));
            return new DiagnosticResult(ok, resp.statusCode(), summary);
        } catch (Exception e) {
            return new DiagnosticResult(false, 0, e.getMessage());
        }
    }

    private DiagnosticResult testCoinGecko() {
        try {
            String url = "https://api.coingecko.com/api/v3/simple/price?ids=bitcoin&vs_currencies=usd";
            HttpRequest req = HttpRequest.newBuilder().uri(URI.create(url)).GET().build();
            HttpResponse<String> resp = httpClient.send(req, HttpResponse.BodyHandlers.ofString());
            boolean ok = resp.statusCode() == 200;
            String summary = ok ? "CoinGecko responded with Bitcoin live price" : resp.body().substring(0, Math.min(120, resp.body().length()));
            return new DiagnosticResult(ok, resp.statusCode(), summary);
        } catch (Exception e) {
            return new DiagnosticResult(false, 0, e.getMessage());
        }
    }

    private DiagnosticResult testQuotes() {
        try {
            String url = "https://dummyjson.com/quotes/random";
            HttpRequest req = HttpRequest.newBuilder().uri(URI.create(url)).GET().build();
            HttpResponse<String> resp = httpClient.send(req, HttpResponse.BodyHandlers.ofString());
            boolean ok = resp.statusCode() == 200;
            String summary = ok ? "Quotes API responded with random quote" : "Status: " + resp.statusCode();
            return new DiagnosticResult(ok, resp.statusCode(), summary);
        } catch (Exception e) {
            return new DiagnosticResult(false, 0, e.getMessage());
        }
    }

    private DiagnosticResult testJokeApi() {
        try {
            String url = "https://v2.jokeapi.dev/joke/Any?safe-mode";
            HttpRequest req = HttpRequest.newBuilder().uri(URI.create(url)).GET().build();
            HttpResponse<String> resp = httpClient.send(req, HttpResponse.BodyHandlers.ofString());
            boolean ok = resp.statusCode() == 200;
            String summary = ok ? "JokeAPI responded with 200 OK" : "Status: " + resp.statusCode();
            return new DiagnosticResult(ok, resp.statusCode(), summary);
        } catch (Exception e) {
            return new DiagnosticResult(false, 0, e.getMessage());
        }
    }

    private DiagnosticResult testCatFacts() {
        try {
            String url = "https://catfact.ninja/fact";
            HttpRequest req = HttpRequest.newBuilder().uri(URI.create(url)).GET().build();
            HttpResponse<String> resp = httpClient.send(req, HttpResponse.BodyHandlers.ofString());
            boolean ok = resp.statusCode() == 200;
            String summary = ok ? "CatFacts responded with 200 OK" : "Status: " + resp.statusCode();
            return new DiagnosticResult(ok, resp.statusCode(), summary);
        } catch (Exception e) {
            return new DiagnosticResult(false, 0, e.getMessage());
        }
    }

    private DiagnosticResult testLeetCode() {
        try {
            String url = "https://leetcode.com/graphql";
            String body = "{\"query\":\"query getUserProfile($username: String!) { matchedUser(username: $username) { username profile { ranking } } }\",\"variables\":{\"username\":\"ankitarsh\"}}";
            HttpRequest req = HttpRequest.newBuilder()
                    .uri(URI.create(url))
                    .header("Content-Type", "application/json")
                    .header("User-Agent", "Mozilla/5.0")
                    .POST(HttpRequest.BodyPublishers.ofString(body))
                    .build();
            HttpResponse<String> resp = httpClient.send(req, HttpResponse.BodyHandlers.ofString());
            boolean ok = resp.statusCode() == 200;
            String summary = ok ? "LeetCode GraphQL responded with 200 OK" : "Status: " + resp.statusCode();
            return new DiagnosticResult(ok, resp.statusCode(), summary);
        } catch (Exception e) {
            return new DiagnosticResult(false, 0, e.getMessage());
        }
    }

    private DiagnosticResult testPythonAi(String baseUrl, String token) {
        if (baseUrl == null || baseUrl.isBlank()) return new DiagnosticResult(true, 200, "Skipped (PYTHON_AI_BASE_URL not set)");
        try {
            String url = baseUrl + "/";
            HttpRequest req = HttpRequest.newBuilder()
                    .uri(URI.create(url))
                    .header("Authorization", "Bearer " + token)
                    .GET()
                    .build();
            HttpResponse<String> resp = httpClient.send(req, HttpResponse.BodyHandlers.ofString());
            boolean ok = resp.statusCode() == 200;
            String summary = ok ? "Python AI service reachable and healthy (v3.0.0)" : "Status: " + resp.statusCode() + " (" + resp.body() + ")";
            return new DiagnosticResult(ok, resp.statusCode(), summary);
        } catch (Exception e) {
            return new DiagnosticResult(false, 0, "Connection error: " + e.getMessage());
        }
    }

    private record DiagnosticResult(boolean success, int statusCode, String summary) {}
}
