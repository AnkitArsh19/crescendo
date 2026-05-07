package com.crescendo.apps.jobsearch.provider;

import com.crescendo.apps.jobsearch.JobSearchProvider;
import com.crescendo.apps.jobsearch.JobSearchResult;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.core.ParameterizedTypeReference;
import org.springframework.web.client.RestClient;

import java.util.*;

/**
 * Greenhouse Job Board API — public, free, no auth required.
 * <p>
 * Scans specific company boards on Greenhouse for matching jobs.
 * Many Indian tech companies use Greenhouse: Razorpay, Swiggy, CRED,
 * PhonePe, Groww, Meesho, upGrad, etc.
 * <p>
 * Users can provide custom board tokens via {@code greenhouseBoardTokens}
 * config, or we use a curated list of Indian tech companies.
 *
 * @see <a href="https://developers.greenhouse.io/job-board.html">Greenhouse Job Board API docs</a>
 */
public class GreenhouseProvider implements JobSearchProvider {

    private static final Logger log = LoggerFactory.getLogger(GreenhouseProvider.class);
    private static final String API_BASE = "https://boards-api.greenhouse.io/v1/boards/{token}/jobs?content=true";

    /**
     * Default Indian tech companies on Greenhouse.
     * Format: boardToken -> companyName
     */
    private static final Map<String, String> DEFAULT_INDIA_BOARDS = Map.ofEntries(
            Map.entry("razorpay", "Razorpay"),
            Map.entry("swiggy", "Swiggy"),
            Map.entry("caboratechnology", "Ola"),
            Map.entry("cred", "CRED"),
            Map.entry("groww", "Groww"),
            Map.entry("meesho", "Meesho"),
            Map.entry("upgrad", "upGrad"),
            Map.entry("dream11", "Dream11"),
            Map.entry("zeta52", "Zeta"),
            Map.entry("postman", "Postman"),
            Map.entry("browserstack", "BrowserStack"),
            Map.entry("chargebee", "Chargebee"),
            Map.entry("clevertap", "CleverTap"),
            Map.entry("hasaboratechnologies", "Hasura"),
            Map.entry("licious", "Licious")
    );

    @Override public String sourceName() { return "Greenhouse"; }
    @Override public boolean requiresApiKey() { return false; }
    @Override public boolean isEnabled(Map<String, Object> config) { return true; }

    @Override
    @SuppressWarnings("unchecked")
    public List<JobSearchResult> search(String query, String location, Map<String, Object> config) {
        int limit = parseLimit(config, 10);
        String queryLower = query.toLowerCase();

        // Allow user to provide custom board tokens as comma-separated string
        Map<String, String> boards = new LinkedHashMap<>(DEFAULT_INDIA_BOARDS);
        String custom = config != null ? asStr(config.get("greenhouseBoardTokens")) : null;
        if (custom != null && !custom.isBlank()) {
            for (String token : custom.split(",")) {
                String t = token.trim();
                if (!t.isBlank()) boards.put(t, t);
            }
        }

        List<JobSearchResult> allResults = new ArrayList<>();

        for (Map.Entry<String, String> entry : boards.entrySet()) {
            if (allResults.size() >= limit) break;

            String boardToken = entry.getKey();
            String companyName = entry.getValue();

            try {
                Map<String, Object> response = RestClient.create()
                        .get()
                        .uri(API_BASE, boardToken)
                        .retrieve()
                        .body(new ParameterizedTypeReference<>() {});

                if (response == null) continue;

                List<Map<String, Object>> jobs = (List<Map<String, Object>>) response.get("jobs");
                if (jobs == null) continue;

                // Filter by query keywords in title and by location
                List<JobSearchResult> matches = jobs.stream()
                        .filter(job -> {
                            String title = asStr(job.get("title"));
                            return title != null && title.toLowerCase().contains(queryLower);
                        })
                        .filter(job -> {
                            // Apply location filter if a specific location was requested
                            if (location == null || location.isBlank()
                                    || location.equalsIgnoreCase("India")
                                    || location.equalsIgnoreCase("Remote")
                                    || location.equalsIgnoreCase("Anywhere")) {
                                return true;
                            }
                            String jobLoc = null;
                            if (job.get("location") instanceof Map<?, ?> locMap) {
                                jobLoc = asStr(locMap.get("name"));
                            }
                            if (jobLoc == null || jobLoc.isBlank()) return true;
                            String locLower = location.toLowerCase();
                            String jobLocLower = jobLoc.toLowerCase();
                            return jobLocLower.contains(locLower) || locLower.contains(jobLocLower);
                        })
                        .limit(limit - allResults.size())
                        .map(job -> {
                            String jobLoc = null;
                            if (job.get("location") instanceof Map<?, ?> locMap) {
                                jobLoc = asStr(locMap.get("name"));
                            }

                            // Strip HTML from content
                            String desc = asStr(job.get("content"));
                            desc = truncate(stripHtml(desc), 500);

                            List<String> tags = new ArrayList<>();
                            if (job.get("departments") instanceof List<?> depts) {
                                for (Object d : depts) {
                                    if (d instanceof Map<?, ?> dept && dept.get("name") != null) {
                                        tags.add(dept.get("name").toString());
                                    }
                                }
                            }

                            return new JobSearchResult(
                                    asStr(job.get("title")),
                                    companyName,
                                    jobLoc,
                                    asStr(job.get("absolute_url")),
                                    null,
                                    desc,
                                    asStr(job.get("updated_at")),
                                    sourceName(),
                                    tags.isEmpty() ? List.of("Greenhouse") : tags,
                                    null
                            );
                        })
                        .toList();

                allResults.addAll(matches);

            } catch (Exception e) {
                // Don't fail the whole search if one board is down
                log.debug("[job-search] Greenhouse board '{}' failed: {}", boardToken, e.getMessage());
            }
        }

        return allResults;
    }

    private String asStr(Object v) { return v != null ? v.toString() : null; }
    private String truncate(String s, int max) { return s != null && s.length() > max ? s.substring(0, max) + "…" : s; }
    private String stripHtml(String html) { return html != null ? html.replaceAll("<[^>]*>", " ").replaceAll("\\s+", " ").trim() : null; }
    private int parseLimit(Map<String, Object> config, int def) {
        try { return Integer.parseInt(config.getOrDefault("maxResults", def).toString()); }
        catch (Exception e) { return def; }
    }
}
