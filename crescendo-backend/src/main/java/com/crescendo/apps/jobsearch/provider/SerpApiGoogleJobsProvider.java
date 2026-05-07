package com.crescendo.apps.jobsearch.provider;

import com.crescendo.apps.jobsearch.JobSearchProvider;
import com.crescendo.apps.jobsearch.JobSearchResult;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.core.ParameterizedTypeReference;
import org.springframework.web.client.RestClient;

import java.util.*;

/**
 * Google Jobs via SerpAPI — the best single source for Indian jobs because
 * Google aggregates Naukri, LinkedIn, Indeed India, Glassdoor, Foundit, etc.
 *
 * @see <a href="https://serpapi.com/google-jobs-api">SerpAPI Google Jobs docs</a>
 */
public class SerpApiGoogleJobsProvider implements JobSearchProvider {

    private static final Logger log = LoggerFactory.getLogger(SerpApiGoogleJobsProvider.class);
    private static final String BASE_URL = "https://serpapi.com/search.json";

    @Override public String sourceName() { return "Google Jobs (SerpAPI)"; }
    @Override public boolean requiresApiKey() { return true; }

    @Override
    public boolean isEnabled(Map<String, Object> config) {
        String key = config != null ? asStr(config.get("serpApiKey")) : null;
        return key != null && !key.isBlank();
    }

    @Override
    @SuppressWarnings("unchecked")
    public List<JobSearchResult> search(String query, String location, Map<String, Object> config) {
        String apiKey = asStr(config.get("serpApiKey"));
        String loc = (location != null && !location.isBlank()) ? location : "India";
        int limit = parseLimit(config, 10);

        try {
            Map<String, Object> response = RestClient.create()
                    .get()
                    .uri(BASE_URL + "?engine=google_jobs&q={q}&location={loc}&gl=in&hl=en&num={num}&api_key={key}",
                            query, loc, limit, apiKey)
                    .retrieve()
                    .body(new ParameterizedTypeReference<>() {});

            if (response == null) return List.of();

            List<Map<String, Object>> jobs = (List<Map<String, Object>>) response.get("jobs_results");
            if (jobs == null) return List.of();

            return jobs.stream().limit(limit).map(job -> {
                // Extract apply link
                String applyUrl = null;
                if (job.get("apply_options") instanceof List<?> opts && !opts.isEmpty()) {
                    if (opts.get(0) instanceof Map<?, ?> opt) {
                        applyUrl = asStr(opt.get("link"));
                    }
                }
                if (applyUrl == null) applyUrl = asStr(job.get("share_link"));

                // Extract extensions as tags
                List<String> tags = new ArrayList<>();
                if (job.get("detected_extensions") instanceof Map<?, ?> ext) {
                    if (ext.get("schedule_type") != null) tags.add(ext.get("schedule_type").toString());
                    if (Boolean.TRUE.equals(ext.get("work_from_home"))) tags.add("Remote");
                }

                return new JobSearchResult(
                        asStr(job.get("title")),
                        asStr(job.get("company_name")),
                        asStr(job.get("location")),
                        applyUrl,
                        asStr(job.get("salary")),
                        truncate(asStr(job.get("description")), 500),
                        asStr(job.get("detected_extensions") instanceof Map<?, ?> ext2 ? ext2.get("posted_at") : null),
                        sourceName(),
                        tags,
                        tags.isEmpty() ? null : tags.get(0)
                );
            }).toList();

        } catch (Exception e) {
            log.error("[job-search] SerpAPI failed: {}", e.getMessage());
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
