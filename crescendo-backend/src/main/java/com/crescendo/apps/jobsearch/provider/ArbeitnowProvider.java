package com.crescendo.apps.jobsearch.provider;

import com.crescendo.apps.jobsearch.JobSearchProvider;
import com.crescendo.apps.jobsearch.JobSearchResult;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.core.ParameterizedTypeReference;
import org.springframework.web.client.RestClient;

import java.util.*;

/**
 * Arbeitnow — free job board API aggregating jobs from various ATS platforms.
 * No authentication required.
 *
 * @see <a href="https://www.arbeitnow.com/api/job-board-api">Arbeitnow API</a>
 */
public class ArbeitnowProvider implements JobSearchProvider {

    private static final Logger log = LoggerFactory.getLogger(ArbeitnowProvider.class);
    private static final String BASE_URL = "https://www.arbeitnow.com/api/job-board-api";

    @Override public String sourceName() { return "Arbeitnow"; }
    @Override public boolean requiresApiKey() { return false; }
    @Override public boolean isEnabled(Map<String, Object> config) { return true; }

    @Override
    @SuppressWarnings("unchecked")
    public List<JobSearchResult> search(String query, String location, Map<String, Object> config) {
        int limit = parseLimit(config, 10);
        String queryLower = query.toLowerCase();
        String locationLower = (location != null && !location.isBlank()) ? location.toLowerCase() : "";

        try {
            Map<String, Object> response = RestClient.create()
                    .get()
                    .uri(BASE_URL)
                    .retrieve()
                    .body(new ParameterizedTypeReference<>() {});

            if (response == null) return List.of();

            List<Map<String, Object>> jobs = (List<Map<String, Object>>) response.get("data");
            if (jobs == null) return List.of();

            // Arbeitnow doesn't have query or location params — filter client-side
            return jobs.stream()
                    .filter(job -> {
                        String title = asStr(job.get("title"));
                        String desc = asStr(job.get("description"));
                        String company = asStr(job.get("company_name"));
                        String combined = ((title != null ? title : "") + " " + (desc != null ? desc : "") + " " + (company != null ? company : "")).toLowerCase();
                        return combined.contains(queryLower);
                    })
                    .filter(job -> {
                        // Apply location filter if a specific location was requested
                        if (locationLower.isEmpty() || locationLower.equals("remote") || locationLower.equals("anywhere")) {
                            return true;
                        }
                        String jobLoc = asStr(job.get("location"));
                        if (jobLoc == null || jobLoc.isBlank()) return false;
                        String jobLocLower = jobLoc.toLowerCase();
                        return jobLocLower.contains(locationLower) || locationLower.contains(jobLocLower);
                    })
                    .limit(limit)
                    .map(job -> {
                        List<String> tags = new ArrayList<>();
                        if (job.get("tags") instanceof List<?> t) {
                            t.stream().limit(5).forEach(tag -> tags.add(tag.toString()));
                        }
                        if (Boolean.TRUE.equals(job.get("remote"))) tags.add("Remote");

                        return new JobSearchResult(
                                asStr(job.get("title")),
                                asStr(job.get("company_name")),
                                asStr(job.get("location")),
                                asStr(job.get("url")),
                                null,
                                truncate(stripHtml(asStr(job.get("description"))), 500),
                                asStr(job.get("created_at")),
                                sourceName(),
                                tags,
                                Boolean.TRUE.equals(job.get("remote")) ? "Remote" : null
                        );
                    })
                    .toList();

        } catch (Exception e) {
            log.error("[job-search] Arbeitnow failed: {}", e.getMessage());
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

