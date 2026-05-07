package com.crescendo.apps.jobsearch.provider;

import com.crescendo.apps.jobsearch.JobSearchProvider;
import com.crescendo.apps.jobsearch.JobSearchResult;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.core.ParameterizedTypeReference;
import org.springframework.web.client.RestClient;

import java.util.*;

/**
 * Himalayas — free remote jobs API with rich filtering.
 * No authentication required. Supports country-level filtering.
 *
 * @see <a href="https://himalayas.app/jobs/api">Himalayas API</a>
 */
public class HimalayasProvider implements JobSearchProvider {

    private static final Logger log = LoggerFactory.getLogger(HimalayasProvider.class);
    private static final String BASE_URL = "https://himalayas.app/jobs/api";

    @Override public String sourceName() { return "Himalayas"; }
    @Override public boolean requiresApiKey() { return false; }
    @Override public boolean isEnabled(Map<String, Object> config) { return true; }

    @Override
    @SuppressWarnings("unchecked")
    public List<JobSearchResult> search(String query, String location, Map<String, Object> config) {
        int limit = parseLimit(config, 10);

        try {
            // Himalayas supports query params for filtering
            Map<String, Object> response = RestClient.create()
                    .get()
                    .uri(BASE_URL + "?q={q}&limit={limit}", query, Math.min(limit, 50))
                    .retrieve()
                    .body(new ParameterizedTypeReference<>() {});

            if (response == null) return List.of();

            List<Map<String, Object>> jobs = (List<Map<String, Object>>) response.get("jobs");
            if (jobs == null) return List.of();

            return jobs.stream()
                    .filter(job -> {
                        // Apply location filter if a specific location was requested
                        if (location == null || location.isBlank()
                                || location.equalsIgnoreCase("Remote")
                                || location.equalsIgnoreCase("Anywhere")) {
                            return true;
                        }
                        String jobLoc = asStr(job.get("locationRestrictions"));
                        if (jobLoc == null || jobLoc.isBlank()) return true;
                        String locLower = location.toLowerCase();
                        String jobLocLower = jobLoc.toLowerCase();
                        return jobLocLower.contains(locLower) || locLower.contains(jobLocLower)
                                || jobLocLower.contains("worldwide") || jobLocLower.contains("anywhere");
                    })
                    .limit(limit).map(job -> {
                List<String> tags = new ArrayList<>();
                if (job.get("categories") instanceof List<?> cats) {
                    cats.stream().limit(3).forEach(c -> tags.add(c.toString()));
                }
                tags.add("Remote");

                // Seniority
                String seniority = asStr(job.get("seniority"));
                if (seniority != null) tags.add(seniority);

                return new JobSearchResult(
                        asStr(job.get("title")),
                        asStr(job.get("companyName")),
                        asStr(job.get("locationRestrictions")),
                        asStr(job.get("applicationLink")),
                        asStr(job.get("salary")),
                        truncate(stripHtml(asStr(job.get("description"))), 500),
                        asStr(job.get("pubDate")),
                        sourceName(),
                        tags,
                        "Remote"
                );
            }).toList();

        } catch (Exception e) {
            log.error("[job-search] Himalayas failed: {}", e.getMessage());
            return List.of();
        }
    }

    private String asStr(Object v) { return v != null ? v.toString() : null; }
    private String truncate(String s, int max) { return s != null && s.length() > max ? s.substring(0, max) + "…" : s; }
    private String stripHtml(String html) { return html != null ? html.replaceAll("<[^>]*>", " ").replaceAll("\\s+", " ").trim() : null; }
    private int parseLimit(Map<String, Object> config, int def) {
        try { return Integer.parseInt(config.getOrDefault("maxResults", def).toString()); }
        catch (Exception e) { return def; }
    }
}
