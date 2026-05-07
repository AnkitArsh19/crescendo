package com.crescendo.apps.jobsearch.provider;

import com.crescendo.apps.jobsearch.JobSearchProvider;
import com.crescendo.apps.jobsearch.JobSearchResult;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.core.ParameterizedTypeReference;
import org.springframework.http.MediaType;
import org.springframework.web.client.RestClient;

import java.util.*;

/**
 * Jooble — large global job aggregator with India coverage.
 * The API uses POST requests with JSON body.
 *
 * @see <a href="https://jooble.org/api/about">Jooble API</a>
 */
public class JoobleProvider implements JobSearchProvider {

    private static final Logger log = LoggerFactory.getLogger(JoobleProvider.class);
    private static final String BASE_URL = "https://jooble.org/api/";

    @Override public String sourceName() { return "Jooble"; }
    @Override public boolean requiresApiKey() { return true; }

    @Override
    public boolean isEnabled(Map<String, Object> config) {
        String key = config != null ? asStr(config.get("joobleApiKey")) : null;
        return key != null && !key.isBlank();
    }

    @Override
    @SuppressWarnings("unchecked")
    public List<JobSearchResult> search(String query, String location, Map<String, Object> config) {
        String apiKey = asStr(config.get("joobleApiKey"));
        String loc = (location != null && !location.isBlank()) ? location : "India";
        int limit = parseLimit(config, 10);

        try {
            Map<String, Object> body = new HashMap<>();
            body.put("keywords", query);
            body.put("location", loc);
            body.put("page", 1);

            Map<String, Object> response = RestClient.create()
                    .post()
                    .uri(BASE_URL + apiKey)
                    .contentType(MediaType.APPLICATION_JSON)
                    .body(body)
                    .retrieve()
                    .body(new ParameterizedTypeReference<>() {});

            if (response == null) return List.of();

            List<Map<String, Object>> jobs = (List<Map<String, Object>>) response.get("jobs");
            if (jobs == null) return List.of();

            return jobs.stream().limit(limit).map(job -> {
                List<String> tags = new ArrayList<>();
                if (job.get("type") != null) tags.add(job.get("type").toString());

                return new JobSearchResult(
                        asStr(job.get("title")),
                        asStr(job.get("company")),
                        asStr(job.get("location")),
                        asStr(job.get("link")),
                        asStr(job.get("salary")),
                        truncate(asStr(job.get("snippet")), 500),
                        asStr(job.get("updated")),
                        sourceName(),
                        tags,
                        asStr(job.get("type"))
                );
            }).toList();

        } catch (Exception e) {
            log.error("[job-search] Jooble failed: {}", e.getMessage());
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
