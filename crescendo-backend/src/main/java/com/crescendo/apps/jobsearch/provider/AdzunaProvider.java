package com.crescendo.apps.jobsearch.provider;

import com.crescendo.apps.jobsearch.JobSearchProvider;
import com.crescendo.apps.jobsearch.JobSearchResult;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.core.ParameterizedTypeReference;
import org.springframework.web.client.RestClient;

import java.util.*;

/**
 * Adzuna India — job search API with good Indian market coverage.
 * Uses country code {@code in} for India.
 *
 * @see <a href="https://developer.adzuna.com/">Adzuna Developer Portal</a>
 */
public class AdzunaProvider implements JobSearchProvider {

    private static final Logger log = LoggerFactory.getLogger(AdzunaProvider.class);
    private static final String BASE_URL = "https://api.adzuna.com/v1/api/jobs/in/search/1";

    @Override public String sourceName() { return "Adzuna India"; }
    @Override public boolean requiresApiKey() { return true; }

    @Override
    public boolean isEnabled(Map<String, Object> config) {
        String appId = config != null ? asStr(config.get("adzunaAppId")) : null;
        String apiKey = config != null ? asStr(config.get("adzunaApiKey")) : null;
        return appId != null && !appId.isBlank() && apiKey != null && !apiKey.isBlank();
    }

    @Override
    @SuppressWarnings("unchecked")
    public List<JobSearchResult> search(String query, String location, Map<String, Object> config) {
        String appId = asStr(config.get("adzunaAppId"));
        String apiKey = asStr(config.get("adzunaApiKey"));
        int limit = parseLimit(config, 10);

        String loc = (location != null && !location.isBlank()) ? location : "";

        try {
            String uri = BASE_URL + "?app_id={appId}&app_key={key}&what={q}&results_per_page={limit}&content-type=application/json";
            if (!loc.isBlank()) {
                uri += "&where={loc}";
            }

            Map<String, Object> response = RestClient.create()
                    .get()
                    .uri(uri, appId, apiKey, query, limit, loc)
                    .retrieve()
                    .body(new ParameterizedTypeReference<>() {});

            if (response == null) return List.of();

            List<Map<String, Object>> results = (List<Map<String, Object>>) response.get("results");
            if (results == null) return List.of();

            return results.stream().limit(limit).map(job -> {
                // Adzuna location is nested
                String jobLoc = null;
                if (job.get("location") instanceof Map<?, ?> locMap) {
                    if (locMap.get("display_name") != null) {
                        jobLoc = locMap.get("display_name").toString();
                    }
                }

                // Company name is nested
                String company = null;
                if (job.get("company") instanceof Map<?, ?> compMap) {
                    company = asStr(compMap.get("display_name"));
                }

                // Salary
                String salary = null;
                if (job.get("salary_min") != null || job.get("salary_max") != null) {
                    String min = job.get("salary_min") != null ? job.get("salary_min").toString() : "?";
                    String max = job.get("salary_max") != null ? job.get("salary_max").toString() : "?";
                    salary = "₹" + min + " - ₹" + max;
                }

                List<String> tags = new ArrayList<>();
                if (job.get("category") instanceof Map<?, ?> cat) {
                    if (cat.get("label") != null) tags.add(cat.get("label").toString());
                }
                if (job.get("contract_type") != null) tags.add(job.get("contract_type").toString());

                return new JobSearchResult(
                        asStr(job.get("title")),
                        company,
                        jobLoc,
                        asStr(job.get("redirect_url")),
                        salary,
                        truncate(asStr(job.get("description")), 500),
                        asStr(job.get("created")),
                        sourceName(),
                        tags,
                        asStr(job.get("contract_type"))
                );
            }).toList();

        } catch (Exception e) {
            log.error("[job-search] Adzuna failed: {}", e.getMessage());
            return List.of();
        }
    }

    private String asStr(Object v) { return v != null ? v.toString() : null; }
    private String truncate(String s, int max) { return s != null && s.length() > max ? s.substring(0, max) + "…" : s; }
    private int parseLimit(Map<String, Object> config, int def) {
        try { return Integer.parseInt(config.getOrDefault("maxResults", def).toString()); }
        catch (Exception e) { return def; }
    }
}
