package com.crescendo.apps.jobsearch.provider;

import com.crescendo.apps.jobsearch.JobSearchProvider;
import com.crescendo.apps.jobsearch.JobSearchResult;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.core.ParameterizedTypeReference;
import org.springframework.web.client.RestClient;

import java.util.*;

/**
 * Lever Job Board API — public, free, no auth required.
 * <p>
 * Fetches job listings from companies using Lever as their ATS.
 * Several Indian and India-hiring companies use Lever.
 *
 * @see <a href="https://github.com/lever/postings-api">Lever Postings API</a>
 */
public class LeverProvider implements JobSearchProvider {

    private static final Logger log = LoggerFactory.getLogger(LeverProvider.class);
    private static final String API_BASE = "https://api.lever.co/v0/postings/{company}";

    /**
     * Default companies on Lever that hire in India.
     * Format: leverSlug -> companyName
     */
    private static final Map<String, String> DEFAULT_INDIA_BOARDS = Map.of(
            "atlan", "Atlan",
            "incred", "InCred",
            "moengage", "MoEngage",
            "gojek", "GoJek",
            "tokopedia", "Tokopedia",
            "grab", "Grab"
    );

    @Override public String sourceName() { return "Lever"; }
    @Override public boolean requiresApiKey() { return false; }
    @Override public boolean isEnabled(Map<String, Object> config) { return true; }

    @Override
    @SuppressWarnings("unchecked")
    public List<JobSearchResult> search(String query, String location, Map<String, Object> config) {
        int limit = parseLimit(config, 10);
        String queryLower = query.toLowerCase();

        Map<String, String> boards = new LinkedHashMap<>(DEFAULT_INDIA_BOARDS);
        String custom = config != null ? asStr(config.get("leverBoardSlugs")) : null;
        if (custom != null && !custom.isBlank()) {
            for (String slug : custom.split(",")) {
                String s = slug.trim();
                if (!s.isBlank()) boards.put(s, s);
            }
        }

        List<JobSearchResult> allResults = new ArrayList<>();

        for (Map.Entry<String, String> entry : boards.entrySet()) {
            if (allResults.size() >= limit) break;

            String slug = entry.getKey();
            String companyName = entry.getValue();

            try {
                List<Map<String, Object>> jobs = RestClient.create()
                        .get()
                        .uri(API_BASE, slug)
                        .retrieve()
                        .body(new ParameterizedTypeReference<>() {});

                if (jobs == null) continue;

                List<JobSearchResult> matches = jobs.stream()
                        .filter(job -> {
                            String title = asStr(job.get("text"));
                            return title != null && title.toLowerCase().contains(queryLower);
                        })
                        .limit(limit - allResults.size())
                        .map(job -> {
                            String jobLoc = null;
                            if (job.get("categories") instanceof Map<?, ?> cats) {
                                jobLoc = asStr(cats.get("location"));
                            }

                            String desc = asStr(job.get("descriptionPlain"));

                            List<String> tags = new ArrayList<>();
                            if (job.get("categories") instanceof Map<?, ?> cats) {
                                if (cats.get("team") != null) tags.add(cats.get("team").toString());
                                if (cats.get("commitment") != null) tags.add(cats.get("commitment").toString());
                            }
                            tags.add("Lever");

                            return new JobSearchResult(
                                    asStr(job.get("text")),
                                    companyName,
                                    jobLoc,
                                    asStr(job.get("hostedUrl")),
                                    null,
                                    truncate(desc, 500),
                                    asStr(job.get("createdAt")),
                                    sourceName(),
                                    tags,
                                    null
                            );
                        })
                        .toList();

                allResults.addAll(matches);

            } catch (Exception e) {
                log.debug("[job-search] Lever board '{}' failed: {}", slug, e.getMessage());
            }
        }

        return allResults;
    }

    private String asStr(Object v) { return v != null ? v.toString() : null; }
    private String truncate(String s, int max) { return s != null && s.length() > max ? s.substring(0, max) + "…" : s; }
    private int parseLimit(Map<String, Object> config, int def) {
        try { return Integer.parseInt(config.getOrDefault("maxResults", def).toString()); }
        catch (Exception e) { return def; }
    }
}
