package com.crescendo.apps.jobsearch.provider;

import com.crescendo.apps.jobsearch.JobSearchProvider;
import com.crescendo.apps.jobsearch.JobSearchResult;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.core.ParameterizedTypeReference;
import org.springframework.web.client.RestClient;

import java.util.*;

/**
 * Remotive — free remote jobs API, no authentication required.
 * Filters by search query; results may include India-based remote roles.
 *
 * @see <a href="https://remotive.com/api/remote-jobs">Remotive API</a>
 */
public class RemotiveProvider implements JobSearchProvider {

    private static final Logger log = LoggerFactory.getLogger(RemotiveProvider.class);
    private static final String BASE_URL = "https://remotive.com/api/remote-jobs";

    @Override public String sourceName() { return "Remotive"; }
    @Override public boolean requiresApiKey() { return false; }
    @Override public boolean isEnabled(Map<String, Object> config) { return true; }

    @Override
    @SuppressWarnings("unchecked")
    public List<JobSearchResult> search(String query, String location, Map<String, Object> config) {
        int limit = parseLimit(config, 10);

        try {
            Map<String, Object> response = RestClient.create()
                    .get()
                    .uri(BASE_URL + "?search={q}&limit={limit}", query, Math.min(limit, 50))
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
                        String jobLoc = asStr(job.get("candidate_required_location"));
                        if (jobLoc == null || jobLoc.isBlank()) return true;
                        String locLower = location.toLowerCase();
                        String jobLocLower = jobLoc.toLowerCase();
                        // Match if job location contains the requested location or vice versa
                        // Also accept "Worldwide" jobs as they include the requested location
                        return jobLocLower.contains(locLower) || locLower.contains(jobLocLower)
                                || jobLocLower.contains("worldwide") || jobLocLower.contains("anywhere");
                    })
                    .limit(limit).map(job -> {
                List<String> tags = new ArrayList<>();
                if (job.get("category") != null) tags.add(job.get("category").toString());
                if (job.get("job_type") != null) tags.add(job.get("job_type").toString());
                tags.add("Remote");

                return new JobSearchResult(
                        asStr(job.get("title")),
                        asStr(job.get("company_name")),
                        asStr(job.get("candidate_required_location")),
                        asStr(job.get("url")),
                        asStr(job.get("salary")),
                        truncate(stripHtml(asStr(job.get("description"))), 500),
                        asStr(job.get("publication_date")),
                        sourceName(),
                        tags,
                        asStr(job.get("job_type"))
                );
            }).toList();

        } catch (Exception e) {
            log.error("[job-search] Remotive failed: {}", e.getMessage());
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
