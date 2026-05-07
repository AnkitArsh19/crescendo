package com.crescendo.apps.jobsearch;

import com.crescendo.apps.jobsearch.provider.*;
import com.crescendo.execution.action.ActionContext;
import com.crescendo.execution.action.ActionHandler;
import com.crescendo.execution.action.ActionMapping;
import com.crescendo.execution.action.ActionResult;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Value;

import java.util.*;
import java.util.concurrent.*;
import java.util.stream.Collectors;

/**
 * Aggregate job search handler that queries all enabled providers in parallel,
 * deduplicates results, and returns a unified list.
 *
 * <p>Free providers (Remotive, Arbeitnow, Himalayas) always run.
 * Premium providers (SerpAPI, Adzuna, Jooble) run when the platform has
 * configured the corresponding API key in application.properties.
 *
 * <p>Each provider executes with a timeout so one slow source
 * doesn't block the entire search.
 */
@ActionMapping(appKey = "job-search", actionKey = "search-jobs")
public class JobSearchAggregateHandler implements ActionHandler {

    private static final Logger log = LoggerFactory.getLogger(JobSearchAggregateHandler.class);
    private static final int PROVIDER_TIMEOUT_SECONDS = 8;

    // Platform-managed API keys injected from application.properties
    @Value("${crescendo.jobsearch.serpapi-key:}")
    private String serpApiKey;

    @Value("${crescendo.jobsearch.adzuna-app-id:}")
    private String adzunaAppId;

    @Value("${crescendo.jobsearch.adzuna-api-key:}")
    private String adzunaApiKey;

    @Value("${crescendo.jobsearch.jooble-api-key:}")
    private String joobleApiKey;

    /** All known providers, instantiated once. */
    private final List<JobSearchProvider> providers = List.of(
            // -- Free, no-auth (India-focused) --
            new LinkedInGuestProvider(),   // LinkedIn public guest endpoint
            new GreenhouseProvider(),      // Scans 15+ Indian tech company boards (Razorpay, Swiggy, CRED, etc.)
            new LeverProvider(),           // Scans Indian companies on Lever (Atlan, MoEngage, GoJek, etc.)
            // -- Free, no-auth (remote/global) --
            new RemotiveProvider(),
            new ArbeitnowProvider(),
            new HimalayasProvider(),
            // -- Requires API key (platform-managed) --
            new SerpApiGoogleJobsProvider(),  // Google Jobs (aggregates Naukri, Indeed India, Glassdoor)
            new AdzunaProvider(),             // Adzuna India (country=in)
            new JoobleProvider()              // Global aggregator with India coverage
    );

    @Override
    public ActionResult execute(ActionContext context) {
        Map<String, Object> config = context.configuration();

        String rawQuery = config.get("query") != null ? config.get("query").toString().trim() : null;
        if (rawQuery == null || rawQuery.isBlank()) {
            return ActionResult.failure("'query' is required — enter a job title or keywords");
        }

        // Support comma-separated roles: "intern, software developer, apprenticeship"
        String[] keywords = rawQuery.split(",");
        List<String> queries = new ArrayList<>();
        for (String kw : keywords) {
            String trimmed = kw.trim();
            if (!trimmed.isEmpty()) queries.add(trimmed);
        }
        if (queries.isEmpty()) {
            return ActionResult.failure("'query' is required — enter a job title or keywords");
        }

        String location = config.get("location") != null ? config.get("location").toString().trim() : "India";

        // Inject platform-managed API keys into the config map so providers can find them
        Map<String, Object> enrichedConfig = new HashMap<>(config);
        if (serpApiKey != null && !serpApiKey.isBlank()) {
            enrichedConfig.putIfAbsent("serpApiKey", serpApiKey);
        }
        if (adzunaAppId != null && !adzunaAppId.isBlank()) {
            enrichedConfig.putIfAbsent("adzunaAppId", adzunaAppId);
        }
        if (adzunaApiKey != null && !adzunaApiKey.isBlank()) {
            enrichedConfig.putIfAbsent("adzunaApiKey", adzunaApiKey);
        }
        if (joobleApiKey != null && !joobleApiKey.isBlank()) {
            enrichedConfig.putIfAbsent("joobleApiKey", joobleApiKey);
        }

        // Determine which providers are enabled
        List<JobSearchProvider> enabled = providers.stream()
                .filter(p -> p.isEnabled(enrichedConfig))
                .toList();

        if (enabled.isEmpty()) {
            return ActionResult.failure("No job search providers available. Free sources (Remotive, Arbeitnow, Himalayas) should always be enabled.");
        }

        log.info("[job-search] Searching for {} keyword(s): [{}] in '{}' across {} provider(s): {}",
                queries.size(), String.join(", ", queries), location, enabled.size(),
                enabled.stream().map(JobSearchProvider::sourceName).collect(Collectors.joining(", ")));

        // Execute all providers × all keywords in parallel
        ExecutorService executor = Executors.newVirtualThreadPerTaskExecutor();
        List<CompletableFuture<ProviderResult>> futures = new ArrayList<>();

        for (String query : queries) {
            for (JobSearchProvider provider : enabled) {
                futures.add(CompletableFuture.supplyAsync(() -> {
                    try {
                        long start = System.currentTimeMillis();
                        List<JobSearchResult> results = provider.search(query, location, enrichedConfig);
                        long elapsed = System.currentTimeMillis() - start;
                        log.info("[job-search] {} '{}' returned {} results in {}ms",
                                provider.sourceName(), query, results.size(), elapsed);
                        return new ProviderResult(provider.sourceName(), results, null);
                    } catch (Exception e) {
                        log.warn("[job-search] {} '{}' failed: {}", provider.sourceName(), query, e.getMessage());
                        return new ProviderResult(provider.sourceName(), List.of(), e.getMessage());
                    }
                }, executor).orTimeout(PROVIDER_TIMEOUT_SECONDS, TimeUnit.SECONDS)
                 .exceptionally(ex -> {
                     log.warn("[job-search] {} '{}' timed out or errored", provider.sourceName(), query);
                     return new ProviderResult(provider.sourceName(), List.of(), "Timed out after " + PROVIDER_TIMEOUT_SECONDS + "s");
                 }));
            }
        }

        // Collect all results
        List<ProviderResult> allProviderResults = futures.stream()
                .map(CompletableFuture::join)
                .toList();
        executor.shutdown();

        // Merge, deduplicate, sort
        LinkedHashMap<String, JobSearchResult> deduplicated = new LinkedHashMap<>();
        List<Map<String, Object>> sourceStats = new ArrayList<>();

        for (ProviderResult pr : allProviderResults) {
            Map<String, Object> stat = new LinkedHashMap<>();
            stat.put("source", pr.source);
            stat.put("count", pr.results.size());
            if (pr.error != null) stat.put("error", pr.error);
            sourceStats.add(stat);

            for (JobSearchResult result : pr.results) {
                String key = result.deduplicationKey();
                deduplicated.putIfAbsent(key, result);
            }
        }

        // ── Post-filter pipeline (applied AFTER all providers return) ──────────
        // This enforces ALL frontend filters universally across every provider,
        // so results are always relevant regardless of source API capabilities.

        // 1. Title relevance: at least one query keyword must appear in the title
        int beforeTitle = deduplicated.size();
        deduplicated.values().removeIf(r -> !matchesAnyKeywordInTitle(r.title(), queries));
        int afterTitle = deduplicated.size();
        if (beforeTitle != afterTitle) {
            log.info("[job-search] Title filter removed {} irrelevant results ({} → {})",
                    beforeTitle - afterTitle, beforeTitle, afterTitle);
        }

        // 2. Location filter
        int beforeLoc = deduplicated.size();
        deduplicated.values().removeIf(r -> !matchesLocation(r.location(), location));
        int afterLoc = deduplicated.size();
        if (beforeLoc != afterLoc) {
            log.info("[job-search] Location filter '{}' removed {} out-of-area results ({} → {})",
                    location, beforeLoc - afterLoc, beforeLoc, afterLoc);
        }

        // 3. Job type filter (Full-time / Part-time / Contract / Temporary / Internship)
        String jobTypeCode = config.get("linkedInJobType") != null
                ? config.get("linkedInJobType").toString().trim() : "";
        if (!jobTypeCode.isBlank()) {
            int beforeJT = deduplicated.size();
            deduplicated.values().removeIf(r -> !matchesJobType(r, jobTypeCode));
            log.info("[job-search] Job type filter '{}' removed {} results ({} → {})",
                    jobTypeCode, beforeJT - deduplicated.size(), beforeJT, deduplicated.size());
        }

        // 4. Experience level filter (Internship / Entry / Associate / Mid-Senior / Director / Executive)
        String expCode = config.get("linkedInExperience") != null
                ? config.get("linkedInExperience").toString().trim() : "";
        if (!expCode.isBlank()) {
            int beforeExp = deduplicated.size();
            deduplicated.values().removeIf(r -> !matchesExperience(r, expCode));
            log.info("[job-search] Experience filter '{}' removed {} results ({} → {})",
                    expCode, beforeExp - deduplicated.size(), beforeExp, deduplicated.size());
        }

        // 5. Work type filter (On-site / Remote / Hybrid)
        String workTypeCode = config.get("linkedInWorkType") != null
                ? config.get("linkedInWorkType").toString().trim() : "";
        if (!workTypeCode.isBlank()) {
            int beforeWT = deduplicated.size();
            deduplicated.values().removeIf(r -> !matchesWorkType(r, workTypeCode));
            log.info("[job-search] Work type filter '{}' removed {} results ({} → {})",
                    workTypeCode, beforeWT - deduplicated.size(), beforeWT, deduplicated.size());
        }

        // ── End post-filter pipeline ─────────────────────────────────────────────

        // Sort by title ascending (simple, deterministic)
        List<Map<String, Object>> jobList = deduplicated.values().stream()
                .sorted(Comparator.comparing(
                        r -> r.title() != null ? r.title().toLowerCase() : "",
                        String::compareTo))
                .map(this::toMap)
                .toList();

        int maxResults = Math.min(parseLimit(config, 50), 100); // Hard cap at 100
        List<Map<String, Object>> capped = jobList.size() > maxResults
                ? jobList.subList(0, maxResults)
                : jobList;

        Map<String, Object> output = new LinkedHashMap<>();
        output.put("jobs", capped);
        output.put("totalFound", capped.size());
        output.put("totalBeforeDedup", allProviderResults.stream().mapToInt(pr -> pr.results.size()).sum());
        output.put("sources", sourceStats);
        output.put("query", rawQuery);
        output.put("keywords", queries);
        output.put("location", location);

        // Include applied filters in output for transparency
        Map<String, String> appliedFilters = new LinkedHashMap<>();
        appliedFilters.put("location", location);
        if (!jobTypeCode.isBlank()) appliedFilters.put("jobType", jobTypeCode);
        if (!expCode.isBlank()) appliedFilters.put("experienceLevel", expCode);
        if (!workTypeCode.isBlank()) appliedFilters.put("workType", workTypeCode);
        output.put("appliedFilters", appliedFilters);

        log.info("[job-search] Total: {} unique jobs from {} sources (before dedup: {})",
                capped.size(), enabled.size(),
                allProviderResults.stream().mapToInt(pr -> pr.results.size()).sum());

        log.info("[job-search] Jobs aggregated successfully");
        return ActionResult.success(output);
    }

    private Map<String, Object> toMap(JobSearchResult r) {
        Map<String, Object> m = new LinkedHashMap<>();
        m.put("title", r.title());
        m.put("company", r.company());
        m.put("location", r.location());
        m.put("url", r.url());
        if (r.salary() != null) m.put("salary", r.salary());
        if (r.description() != null) m.put("description", r.description());
        if (r.postedDate() != null) m.put("postedDate", r.postedDate());
        m.put("source", r.source());
        if (r.tags() != null && !r.tags().isEmpty()) m.put("tags", r.tags());
        if (r.jobType() != null) m.put("jobType", r.jobType());
        return m;
    }

    private int parseLimit(Map<String, Object> config, int def) {
        try { return Integer.parseInt(config.getOrDefault("maxResults", def).toString()); }
        catch (Exception e) { return def; }
    }

    /**
     * Checks whether a job's location matches the user-requested location.
     * <p>
     * Uses fuzzy matching: normalizes both strings and checks if one contains
     * the other. Also handles common Indian city aliases
     * (Bangalore↔Bengaluru, Mumbai↔Bombay, Chennai↔Madras, Kolkata↔Calcutta,
     * Gurugram↔Gurgaon, Thiruvananthapuram↔Trivandrum).
     * <p>
     * Broad locations like "India", "Remote", or blank values always match.
     */
    private boolean matchesLocation(String jobLocation, String requestedLocation) {
        // If no location filter or very broad filter, everything matches
        if (requestedLocation == null || requestedLocation.isBlank()) return true;
        String reqNorm = normalize(requestedLocation);
        if (reqNorm.isEmpty() || reqNorm.equals("india") || reqNorm.equals("remote") || reqNorm.equals("anywhere")) {
            return true;
        }

        // If job has no location info, keep it (benefit of doubt)
        if (jobLocation == null || jobLocation.isBlank()) return true;
        String jobNorm = normalize(jobLocation);
        if (jobNorm.isEmpty()) return true;

        // "Remote" jobs that mention the matching country/region still pass
        if (jobNorm.equals("remote") || jobNorm.contains("remote")) {
            // If the job says "Remote" but also lists a country, check the country
            if (jobNorm.contains(reqNorm)) return true;
            // Pure "Remote" with no country — allow only if user asked for remote or broad India
            return jobNorm.equals("remote");
        }

        // Direct substring match
        if (jobNorm.contains(reqNorm) || reqNorm.contains(jobNorm)) return true;

        // Check aliases (both directions)
        for (String alias : getAliases(reqNorm)) {
            if (jobNorm.contains(alias)) return true;
        }

        return false;
    }

    private String normalize(String s) {
        return s.toLowerCase()
                .replaceAll("[^a-z0-9 ]", " ")
                .replaceAll("\\s+", " ")
                .trim();
    }

    /** Returns known aliases for common Indian city names. */
    private List<String> getAliases(String normalized) {
        Map<String, List<String>> aliasMap = Map.ofEntries(
                Map.entry("bangalore", List.of("bengaluru", "blr")),
                Map.entry("bengaluru", List.of("bangalore", "blr")),
                Map.entry("mumbai", List.of("bombay")),
                Map.entry("bombay", List.of("mumbai")),
                Map.entry("chennai", List.of("madras")),
                Map.entry("madras", List.of("chennai")),
                Map.entry("kolkata", List.of("calcutta")),
                Map.entry("calcutta", List.of("kolkata")),
                Map.entry("gurugram", List.of("gurgaon")),
                Map.entry("gurgaon", List.of("gurugram")),
                Map.entry("thiruvananthapuram", List.of("trivandrum")),
                Map.entry("trivandrum", List.of("thiruvananthapuram")),
                Map.entry("noida", List.of("greater noida", "noida")),
                Map.entry("hyderabad", List.of("secunderabad")),
                Map.entry("pune", List.of("pimpri", "chinchwad")),
                Map.entry("delhi", List.of("new delhi", "ncr")),
                Map.entry("new delhi", List.of("delhi", "ncr")),
                Map.entry("ncr", List.of("delhi", "new delhi", "noida", "gurgaon", "gurugram"))
        );
        return aliasMap.getOrDefault(normalized, List.of());
    }

    // ── Title Relevance Filter ───────────────────────────────────────────────

    /**
     * Returns true if the job title contains at least one of the query keywords.
     * This prevents jobs that only mention the keyword in the description from
     * polluting results (e.g. "COO Associate" when searching for "Intern").
     */
    private boolean matchesAnyKeywordInTitle(String title, List<String> keywords) {
        if (title == null || title.isBlank()) return false;
        String titleLower = title.toLowerCase();
        for (String keyword : keywords) {
            if (titleLower.contains(keyword.toLowerCase())) return true;
        }
        return false;
    }

    // ── Job Type Filter ──────────────────────────────────────────────────────

    /**
     * Checks if a job matches the selected job type.
     * Maps LinkedIn codes (F, P, C, T, I) to keywords and checks
     * the job's title, tags, and jobType field.
     */
    private boolean matchesJobType(JobSearchResult r, String code) {
        List<String> keywords = switch (code.toUpperCase()) {
            case "F" -> List.of("full-time", "full time", "fulltime", "permanent");
            case "P" -> List.of("part-time", "part time", "parttime");
            case "C" -> List.of("contract", "freelance", "contractor");
            case "T" -> List.of("temporary", "temp");
            case "I" -> List.of("internship", "intern", "apprentice", "trainee");
            default -> List.of();
        };
        if (keywords.isEmpty()) return true;

        String searchable = toSearchableText(r);
        for (String kw : keywords) {
            if (searchable.contains(kw)) return true;
        }
        return false;
    }

    // ── Experience Level Filter ──────────────────────────────────────────────

    /**
     * Checks if a job matches the selected experience level.
     * Maps LinkedIn codes (1–6) to keywords and checks title/tags.
     */
    private boolean matchesExperience(JobSearchResult r, String code) {
        List<String> keywords = switch (code) {
            case "1" -> List.of("intern", "internship", "apprentice", "trainee");
            case "2" -> List.of("entry", "entry-level", "junior", "fresher", "graduate");
            case "3" -> List.of("associate", "mid-level");
            case "4" -> List.of("senior", "mid-senior", "lead", "staff", "principal");
            case "5" -> List.of("director", "head of", "vp");
            case "6" -> List.of("executive", "cxo", "ceo", "cto", "cfo", "coo", "vp");
            default -> List.of();
        };
        if (keywords.isEmpty()) return true;

        String searchable = toSearchableText(r);
        for (String kw : keywords) {
            if (searchable.contains(kw)) return true;
        }
        return false;
    }

    // ── Work Type Filter ─────────────────────────────────────────────────────

    /**
     * Checks if a job matches the selected work type (On-site / Remote / Hybrid).
     * Maps LinkedIn codes (1, 2, 3) and checks location, tags, and jobType.
     */
    private boolean matchesWorkType(JobSearchResult r, String code) {
        List<String> keywords = switch (code) {
            case "1" -> List.of("on-site", "onsite", "on site", "office");
            case "2" -> List.of("remote", "work from home", "wfh", "distributed");
            case "3" -> List.of("hybrid");
            default -> List.of();
        };
        if (keywords.isEmpty()) return true;

        String searchable = toSearchableText(r);
        for (String kw : keywords) {
            if (searchable.contains(kw)) return true;
        }
        return false;
    }

    /**
     * Concatenates all searchable text from a job result into a single lowercase
     * string for filter matching. Includes title, location, tags, and jobType.
     */
    private String toSearchableText(JobSearchResult r) {
        StringBuilder sb = new StringBuilder();
        if (r.title() != null) sb.append(r.title()).append(' ');
        if (r.location() != null) sb.append(r.location()).append(' ');
        if (r.jobType() != null) sb.append(r.jobType()).append(' ');
        if (r.tags() != null) {
            for (String tag : r.tags()) sb.append(tag).append(' ');
        }
        return sb.toString().toLowerCase();
    }

    /** Internal record to collect per-provider results. */
    private record ProviderResult(String source, List<JobSearchResult> results, String error) {}
}

