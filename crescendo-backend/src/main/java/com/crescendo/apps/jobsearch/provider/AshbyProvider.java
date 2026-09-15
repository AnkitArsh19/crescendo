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
 * Ashby Job Board API provider: public, free, no authentication required.
 * <p>
 * Fetches real-time job listings from modern AI research labs and technology companies
 * that use Ashby as their primary ATS (e.g. OpenAI, Perplexity, Linear, Ramp, Vercel).
 * All boards are queried concurrently using Java Virtual Threads.
 *
 * @see <a href="https://developers.ashbyhq.com/reference/jobboardapiobject">Ashby Job Board API</a>
 */
public class AshbyProvider implements JobSearchProvider {

    private static final Logger log = LoggerFactory.getLogger(AshbyProvider.class);
    private static final String API_BASE = "https://api.ashbyhq.com/posting-api/job-board/{company}";

    private static final Pattern INTERN_PATTERN = Pattern.compile(
            "\\b(intern|internship|co-op|coop|trainee|summer\\s+intern)\\b",
            Pattern.CASE_INSENSITIVE
    );

    /**
     * Curated list of high-growth tech companies and AI labs using Ashby.
     * Format: ashbySlug -> companyName
     */
    private static final Map<String, String> DEFAULT_BOARDS = Map.ofEntries(
            Map.entry("openai", "OpenAI"),
            Map.entry("perplexity", "Perplexity"),
            Map.entry("linear", "Linear"),
            Map.entry("ramp", "Ramp"),
            Map.entry("vercel", "Vercel"),
            Map.entry("retool", "Retool"),
            Map.entry("character", "Character.ai"),
            Map.entry("sentry", "Sentry"),
            Map.entry("cursor", "Cursor (Anysphere)"),
            Map.entry("quora", "Quora"),
            Map.entry("together", "Together AI"),
            Map.entry("postman", "Postman")
    );

    @Override
    public String sourceName() {
        return "Ashby";
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
    @SuppressWarnings("unchecked")
    public List<JobSearchResult> search(String query, String location, Map<String, Object> config) {
        int limit = parseLimit(config, 20);
        String queryLower = query != null ? query.toLowerCase() : "";
        boolean internshipOnly = isInternshipOnly(config);

        Map<String, String> boards = new LinkedHashMap<>(DEFAULT_BOARDS);
        String custom = config != null ? asStr(config.get("ashbyBoardSlugs")) : null;
        if (custom != null && !custom.isBlank()) {
            for (String slug : custom.split(",")) {
                String s = slug.trim();
                if (!s.isBlank()) boards.put(s, s);
            }
        }

        // Query all boards concurrently using Virtual Threads
        List<CompletableFuture<List<JobSearchResult>>> futures = new ArrayList<>();
        ExecutorService executor = Executors.newVirtualThreadPerTaskExecutor();
        try {
            for (Map.Entry<String, String> entry : boards.entrySet()) {
                String slug = entry.getKey();
                String companyName = entry.getValue();

                futures.add(CompletableFuture.supplyAsync(() -> queryBoard(slug, companyName, queryLower, location, internshipOnly), executor));
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
            Map<String, Object> response = HTTP
                    .get()
                    .uri(API_BASE, slug)
                    .retrieve()
                    .body(new ParameterizedTypeReference<>() {});

            if (response == null || !(response.get("jobs") instanceof List<?> rawJobs)) {
                return Collections.emptyList();
            }

            List<JobSearchResult> results = new ArrayList<>();
            for (Object item : rawJobs) {
                if (!(item instanceof Map<?, ?> job)) continue;

                String title = asStr(job.get("title"));
                if (title == null) continue;

                // Keyword match
                if (!queryLower.isBlank() && !title.toLowerCase().contains(queryLower)) {
                    continue;
                }

                // Internship-only filter
                if (internshipOnly && !INTERN_PATTERN.matcher(title).find()) {
                    continue;
                }

                String jobLoc = asStr(job.get("location"));
                boolean isRemote = Boolean.TRUE.equals(job.get("isRemote"));

                // Location match
                if (!matchesLocation(location, jobLoc, isRemote)) {
                    continue;
                }

                String url = asStr(job.get("jobUrl"));
                String desc = asStr(job.get("descriptionPlain"));
                if (desc != null && desc.length() > 500) {
                    desc = desc.substring(0, 500) + "...";
                }
                String publishedAt = asStr(job.get("publishedAt"));
                String department = asStr(job.get("department"));
                String empType = asStr(job.get("employmentType"));

                List<String> tags = new ArrayList<>();
                tags.add("Ashby");
                if (department != null && !department.isBlank()) tags.add(department);
                if (isRemote) tags.add("Remote");

                results.add(new JobSearchResult(
                        title,
                        companyName,
                        jobLoc != null ? jobLoc : (isRemote ? "Remote" : "Unspecified"),
                        url,
                        null,
                        desc,
                        publishedAt,
                        sourceName(),
                        tags,
                        empType
                ));
            }
            return results;
        } catch (Exception e) {
            log.debug("[job-search] Ashby board '{}' failed: {}", slug, e.getMessage());
            return Collections.emptyList();
        }
    }

    private boolean matchesLocation(String requestedLoc, String jobLoc, boolean isRemote) {
        if (requestedLoc == null || requestedLoc.isBlank()
                || requestedLoc.equalsIgnoreCase("India")
                || requestedLoc.equalsIgnoreCase("Anywhere")
                || requestedLoc.equalsIgnoreCase("All")) {
            return true;
        }
        if (requestedLoc.equalsIgnoreCase("Remote")) {
            return isRemote || (jobLoc != null && jobLoc.toLowerCase().contains("remote"));
        }
        if (jobLoc == null) return true;
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
    private int parseLimit(Map<String, Object> config, int def) {
        try { return Integer.parseInt(config.getOrDefault("maxResults", def).toString()); }
        catch (Exception e) { return def; }
    }
}
