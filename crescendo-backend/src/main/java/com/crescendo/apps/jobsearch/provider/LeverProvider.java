package com.crescendo.apps.jobsearch.provider;

import com.crescendo.apps.jobsearch.JobSearchProvider;
import com.crescendo.apps.jobsearch.JobSearchResult;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.core.ParameterizedTypeReference;
import org.springframework.web.client.RestClient;

import java.util.*;
import java.util.concurrent.*;
import java.util.regex.Pattern;

/**
 * Lever Job Board API provider: public, free, no authentication required.
 * <p>
 * Fetches job listings from companies using Lever as their ATS, querying all
 * boards concurrently using Java Virtual Threads.
 *
 * @see <a href="https://github.com/lever/postings-api">Lever Postings API</a>
 */
public class LeverProvider implements JobSearchProvider {

    private static final Logger log = LoggerFactory.getLogger(LeverProvider.class);
    private static final String API_BASE = "https://api.lever.co/v0/postings/{company}";

    private static final Pattern INTERN_PATTERN = Pattern.compile(
            "\\b(intern|internship|co-op|coop|trainee|summer\\s+intern)\\b",
            Pattern.CASE_INSENSITIVE
    );

    /**
     * Curated list of companies on Lever hiring in India and globally.
     * Format: leverSlug -> companyName
     */
    private static final Map<String, String> DEFAULT_BOARDS = Map.ofEntries(
            Map.entry("atlan", "Atlan"),
            Map.entry("incred", "InCred"),
            Map.entry("moengage", "MoEngage"),
            Map.entry("gojek", "Gojek"),
            Map.entry("tokopedia", "Tokopedia"),
            Map.entry("grab", "Grab"),
            Map.entry("canva", "Canva"),
            Map.entry("netflix", "Netflix"),
            Map.entry("palantir", "Palantir"),
            Map.entry("datadoghq", "Datadog")
    );

    @Override
    public String sourceName() {
        return "Lever";
    }

    @Override
    public boolean requiresApiKey() {
        return false;
    }

    @Override
    public boolean isEnabled(Map<String, Object> config) {
        return true;
    }

    @Override
    public List<JobSearchResult> search(String query, String location, Map<String, Object> config) {
        int limit = parseLimit(config, 20);
        String queryLower = query != null ? query.toLowerCase() : "";
        boolean internshipOnly = isInternshipOnly(config);

        Map<String, String> boards = new LinkedHashMap<>(DEFAULT_BOARDS);
        String custom = config != null ? asStr(config.get("leverBoardSlugs")) : null;
        if (custom != null && !custom.isBlank()) {
            for (String slug : custom.split(",")) {
                String s = slug.trim();
                if (!s.isBlank()) {
                    boards.put(s, s);
                }
            }
        }

        // Query all company boards in parallel using Virtual Threads
        List<CompletableFuture<List<JobSearchResult>>> futures = new ArrayList<>();
        ExecutorService executor = Executors.newVirtualThreadPerTaskExecutor();
        try {
            for (Map.Entry<String, String> entry : boards.entrySet()) {
                String slug = entry.getKey();
                String companyName = entry.getValue();

                futures.add(CompletableFuture.supplyAsync(() ->
                        queryBoard(slug, companyName, queryLower, location, internshipOnly), executor));
            }

            try {
                CompletableFuture.allOf(futures.toArray(new CompletableFuture[0]))
                        .get(6, TimeUnit.SECONDS);
            } catch (Exception ignored) {
                // Global 6s timeout: collect all boards that finished
            }

            List<JobSearchResult> allResults = new ArrayList<>();
            for (var future : futures) {
                if (future.isDone() && !future.isCompletedExceptionally()) {
                    try {
                        allResults.addAll(future.getNow(Collections.emptyList()));
                        if (allResults.size() >= limit) break;
                    } catch (Exception ignored) {}
                }
            }

            if (allResults.size() > limit) {
                return allResults.subList(0, limit);
            }
            return allResults;
        } finally {
            executor.shutdownNow();
        }
    }

    private static final RestClient HTTP = RestClient.create();

    @SuppressWarnings("unchecked")
    private List<JobSearchResult> queryBoard(String slug, String companyName, String queryLower, String location, boolean internshipOnly) {
        try {
            List<Map<String, Object>> jobs = HTTP
                    .get()
                    .uri(API_BASE, slug)
                    .retrieve()
                    .body(new ParameterizedTypeReference<>() {});

            if (jobs == null) return Collections.emptyList();

            List<JobSearchResult> results = new ArrayList<>();
            for (Map<String, Object> job : jobs) {
                String title = asStr(job.get("text"));
                if (title == null) continue;

                // Keyword match
                if (!queryLower.isBlank() && !title.toLowerCase().contains(queryLower)) {
                    continue;
                }

                // Internship-only filter
                if (internshipOnly && !INTERN_PATTERN.matcher(title).find()) {
                    continue;
                }

                String jobLoc = null;
                if (job.get("categories") instanceof Map<?, ?> cats) {
                    jobLoc = asStr(cats.get("location"));
                }

                // Location match
                if (!matchesLocation(location, jobLoc)) {
                    continue;
                }

                String desc = asStr(job.get("descriptionPlain"));

                List<String> tags = new ArrayList<>();
                if (job.get("categories") instanceof Map<?, ?> cats) {
                    if (cats.get("team") != null) tags.add(cats.get("team").toString());
                    if (cats.get("commitment") != null) tags.add(cats.get("commitment").toString());
                }
                tags.add("Lever");

                results.add(new JobSearchResult(
                        title,
                        companyName,
                        jobLoc != null ? jobLoc : "Unspecified",
                        asStr(job.get("hostedUrl")),
                        null,
                        truncate(desc, 500),
                        asStr(job.get("createdAt")),
                        sourceName(),
                        tags,
                        null
                ));
            }
            return results;
        } catch (Exception e) {
            log.debug("[job-search] Lever board '{}' failed: {}", slug, e.getMessage());
            return Collections.emptyList();
        }
    }

    private boolean matchesLocation(String requestedLoc, String jobLoc) {
        if (requestedLoc == null || requestedLoc.isBlank()
                || requestedLoc.equalsIgnoreCase("India")
                || requestedLoc.equalsIgnoreCase("Remote")
                || requestedLoc.equalsIgnoreCase("Anywhere")
                || requestedLoc.equalsIgnoreCase("All")) {
            return true;
        }
        if (jobLoc == null || jobLoc.isBlank()) return true;
        String req = requestedLoc.toLowerCase();
        String actual = jobLoc.toLowerCase();
        return actual.contains(req) || req.contains(actual);
    }

    private boolean isInternshipOnly(Map<String, Object> config) {
        if (config == null) return false;
        Object val = config.get("internshipOnly");
        if (val instanceof Boolean b) return b;
        return val != null && Boolean.parseBoolean(val.toString());
    }

    private String asStr(Object v) { return v != null ? v.toString() : null; }
    private String truncate(String s, int max) { return s != null && s.length() > max ? s.substring(0, max) + "..." : s; }
    private int parseLimit(Map<String, Object> config, int def) {
        try { return Integer.parseInt(config.getOrDefault("maxResults", def).toString()); }
        catch (Exception e) { return def; }
    }
}
