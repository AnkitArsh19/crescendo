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
    private static final int PROVIDER_TIMEOUT_SECONDS = 25;

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
            // -- Free, no-auth (India-focused & ATS) --
            new LinkedInGuestProvider(),   // LinkedIn public guest endpoint
            new GreenhouseProvider(),      // Scans 45+ company boards (Razorpay, Swiggy, CRED, Anthropic, Citadel, etc.)
            new AshbyProvider(),           // Scans Ashby boards (OpenAI, Perplexity, Linear, Ramp, Vercel, etc.)
            new LeverProvider(),           // Scans Lever company boards (Atlan, MoEngage, GoJek, Netflix, etc.)
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

        List<String> queries = parseTags(config.get("query"));
        if (queries.isEmpty()) {
            return ActionResult.failure("'query' is required: enter a job title or keywords");
        }

        List<String> locTags = parseTags(config.get("location"));
        String location = locTags.isEmpty() ? "India" : String.join(", ", locTags);

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

        for (ProviderResult pr : allProviderResults) {
            for (JobSearchResult result : pr.results) {
                String key = result.deduplicationKey();
                deduplicated.putIfAbsent(key, result);
            }
        }

        // Aggregate source stats per provider (sum across keywords)
        Map<String, Map<String, Object>> statsMap = new LinkedHashMap<>();
        for (ProviderResult pr : allProviderResults) {
            statsMap.compute(pr.source, (src, existing) -> {
                if (existing == null) {
                    Map<String, Object> stat = new LinkedHashMap<>();
                    stat.put("source", src);
                    stat.put("count", pr.results.size());
                    if (pr.error != null) stat.put("error", pr.error);
                    return stat;
                } else {
                    existing.put("count", (int) existing.get("count") + pr.results.size());
                    if (pr.error != null && !existing.containsKey("error")) {
                        existing.put("error", pr.error);
                    }
                    return existing;
                }
            });
        }
        List<Map<String, Object>> sourceStats = new ArrayList<>(statsMap.values());

        // ── Post-filter pipeline (applied AFTER all providers return) ──────────
        // This enforces ALL frontend filters universally across every provider,
        // so results are always relevant regardless of source API capabilities.

        // 0. Internship-only filter
        boolean internshipOnly = Boolean.parseBoolean(String.valueOf(config.getOrDefault("internshipOnly", "false")));
        if (internshipOnly) {
            int beforeIntern = deduplicated.size();
            deduplicated.values().removeIf(r -> !matchesInternship(r));
            int afterIntern = deduplicated.size();
            if (beforeIntern != afterIntern) {
                log.info("[job-search] Internship filter removed {} non-internship results ({} -> {})",
                        beforeIntern - afterIntern, beforeIntern, afterIntern);
            }
        }

        // 1. Title relevance: at least one query keyword must appear in the title
        int beforeTitle = deduplicated.size();
        deduplicated.values().removeIf(r -> !matchesAnyKeywordInTitle(r.title(), queries));
        int afterTitle = deduplicated.size();
        if (beforeTitle != afterTitle) {
            log.info("[job-search] Title filter removed {} irrelevant results ({} -> {})",
                    beforeTitle - afterTitle, beforeTitle, afterTitle);
        }

        // 2. Location filter
        int beforeLoc = deduplicated.size();
        deduplicated.values().removeIf(r -> !matchesLocation(r.location(), location));
        int afterLoc = deduplicated.size();
        if (beforeLoc != afterLoc) {
            log.info("[job-search] Location filter '{}' removed {} out-of-area results ({} -> {})",
                    location, beforeLoc - afterLoc, beforeLoc, afterLoc);
        }

        // 3. Job type filter (Full-time / Part-time / Contract / Temporary / Internship)
        String jobTypeCode = config.get("linkedInJobType") != null
                ? config.get("linkedInJobType").toString().trim() : "";
        if (!jobTypeCode.isBlank()) {
            int beforeJT = deduplicated.size();
            deduplicated.values().removeIf(r -> !matchesJobType(r, jobTypeCode));
            log.info("[job-search] Job type filter '{}' removed {} results ({} -> {})",
                    jobTypeCode, beforeJT - deduplicated.size(), beforeJT, deduplicated.size());
        }

        // 4. Experience level filter (Internship / Entry / Associate / Mid-Senior / Director / Executive)
        String expCode = config.get("linkedInExperience") != null
                ? config.get("linkedInExperience").toString().trim() : "";
        if (!expCode.isBlank()) {
            int beforeExp = deduplicated.size();
            deduplicated.values().removeIf(r -> !matchesExperience(r, expCode));
            log.info("[job-search] Experience filter '{}' removed {} results ({} -> {})",
                    expCode, beforeExp - deduplicated.size(), beforeExp, deduplicated.size());
        }

        // 5. Work type filter (On-site / Remote / Hybrid)
        String workTypeCode = config.get("linkedInWorkType") != null
                ? config.get("linkedInWorkType").toString().trim() : "";
        if (!workTypeCode.isBlank()) {
            int beforeWT = deduplicated.size();
            deduplicated.values().removeIf(r -> !matchesWorkType(r, workTypeCode));
            log.info("[job-search] Work type filter '{}' removed {} results ({} -> {})",
                    workTypeCode, beforeWT - deduplicated.size(), beforeWT, deduplicated.size());
        }

        // 6. Target company filter (Curated 150+ Tier-1, Quant, AI, and GCC companies)
        boolean targetCompaniesOnly = Boolean.parseBoolean(String.valueOf(config.getOrDefault("targetCompaniesOnly", "false")));
        String customWhitelistRaw = config.get("customTargetCompanies") != null
                ? config.get("customTargetCompanies").toString().trim() : "";
        Object rawCategories = config.get("targetCompanyCategories");

        Set<String> selectedCategories = new HashSet<>();
        if (rawCategories instanceof Collection<?> coll) {
            for (Object item : coll) {
                if (item != null) selectedCategories.add(item.toString().trim());
            }
        } else if (rawCategories instanceof String s && !s.isBlank()) {
            for (String part : s.split(",")) {
                String p = part.trim();
                if (!p.isEmpty()) selectedCategories.add(p);
            }
        }

        Set<String> customWhitelist = new HashSet<>();
        if (!customWhitelistRaw.isBlank()) {
            for (String name : customWhitelistRaw.split(",")) {
                String n = name.trim();
                if (!n.isEmpty()) customWhitelist.add(n);
            }
        }

        boolean applyCompanyFilter = targetCompaniesOnly || !selectedCategories.isEmpty() || !customWhitelist.isEmpty();
        if (applyCompanyFilter) {
            int beforeComp = deduplicated.size();
            deduplicated.values().removeIf(r -> !TargetCompanyRegistry.matches(
                    r.company(),
                    r.url(),
                    selectedCategories,
                    customWhitelist
            ));
            int afterComp = deduplicated.size();
            if (beforeComp != afterComp) {
                log.info("[job-search] Target company filter removed {} non-target results ({} -> {})",
                        beforeComp - afterComp, beforeComp, afterComp);
            }
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
        output.put("query", String.join(", ", queries));
        output.put("keywords", queries);
        output.put("location", location);

        // Include applied filters in output for transparency
        Map<String, String> appliedFilters = new LinkedHashMap<>();
        appliedFilters.put("location", location);
        if (internshipOnly) appliedFilters.put("internshipOnly", "true");
        if (targetCompaniesOnly) appliedFilters.put("targetCompaniesOnly", "true");
        if (!selectedCategories.isEmpty()) appliedFilters.put("targetCompanyCategories", String.join(", ", selectedCategories));
        if (!customWhitelist.isEmpty()) appliedFilters.put("customTargetCompanies", String.join(", ", customWhitelist));
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
        if (reqNorm.isEmpty() || reqNorm.equals("remote") || reqNorm.equals("anywhere")) {
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
            // Pure "Remote" with no country: allow only if user asked for remote or broad India
            return jobNorm.equals("remote");
        }

        // For broad country-level filter like "India", accept jobs that:
        // - contain "india" explicitly, OR
        // - contain a known Indian city/state, OR
        // - don't mention any clearly foreign location
        if (reqNorm.equals("india")) {
            if (jobNorm.contains("india")) return true;
            // Check if job location contains a known Indian city or state
            if (containsIndianLocation(jobNorm)) return true;
            // Reject jobs with explicitly foreign locations (cities/countries not in India)
            if (containsForeignLocation(jobNorm)) return false;
            // Ambiguous location (e.g. just a company name) — give benefit of doubt
            return true;
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

    /** Checks if the normalized location string contains a known Indian city or state. */
    private boolean containsIndianLocation(String normalized) {
        List<String> indianLocations = List.of(
                "bangalore", "bengaluru", "mumbai", "bombay", "delhi", "new delhi",
                "hyderabad", "chennai", "madras", "pune", "kolkata", "calcutta",
                "gurugram", "gurgaon", "noida", "greater noida", "ahmedabad",
                "jaipur", "lucknow", "chandigarh", "kochi", "coimbatore",
                "thiruvananthapuram", "trivandrum", "indore", "bhopal", "nagpur",
                "visakhapatnam", "vizag", "mangalore", "mangaluru", "mysore",
                "mysuru", "surat", "vadodara", "rajkot", "bhubaneswar",
                "patna", "ranchi", "dehradun", "shimla", "varanasi", "agra",
                "jabalpur", "sehore", "kanpur",
                "karnataka", "maharashtra", "telangana", "tamil nadu",
                "haryana", "uttar pradesh", "madhya pradesh", "rajasthan",
                "gujarat", "west bengal", "kerala", "andhra pradesh",
                "odisha", "bihar", "jharkhand", "punjab", "chhattisgarh",
                "uttarakhand", "goa", "ncr", "blr"
        );
        for (String loc : indianLocations) {
            if (normalized.contains(loc)) return true;
        }
        return false;
    }

    /** Checks if the normalized location string contains a clearly non-Indian location. */
    private boolean containsForeignLocation(String normalized) {
        List<String> foreignLocations = List.of(
                "berlin", "london", "new york", "san francisco", "paris",
                "tokyo", "singapore", "toronto", "sydney", "melbourne",
                "amsterdam", "dublin", "zurich", "munich", "seattle",
                "chicago", "boston", "austin", "los angeles", "denver",
                "belgrade", "warsaw", "prague", "vienna", "barcelona",
                "stockholm", "copenhagen", "oslo", "helsinki", "lisbon",
                "milan", "rome", "madrid", "brussels", "frankfurt",
                "beijing", "shanghai", "shenzhen", "hong kong", "seoul",
                "jakarta", "bangkok", "kuala lumpur", "manila", "dubai",
                "abu dhabi", "riyadh", "tel aviv", "cairo", "nairobi",
                "cape town", "lagos", "sao paulo", "buenos aires", "mexico city",
                "vancouver", "montreal", "ottawa", "calgary",
                // Countries
                "united states", "united kingdom", "germany", "france",
                "japan", "australia", "canada", "brazil", "china",
                "south korea", "indonesia", "thailand", "malaysia",
                "philippines", "vietnam", "uae", "saudi arabia",
                "israel", "egypt", "nigeria", "south africa", "mexico",
                "argentina", "colombia"
        );
        for (String loc : foreignLocations) {
            if (normalized.contains(loc)) return true;
        }
        return false;
    }

    // ── Internship Filter ───────────────────────────────────────────────────

    private static final java.util.regex.Pattern INTERN_PATTERN = java.util.regex.Pattern.compile(
            "\\b(intern|internship|co-op|coop|trainee|summer\\s+intern)\\b",
            java.util.regex.Pattern.CASE_INSENSITIVE
    );

    private boolean matchesInternship(JobSearchResult r) {
        String searchable = toSearchableText(r);
        return INTERN_PATTERN.matcher(searchable).find();
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
     * Maps LinkedIn codes (1 to 6) to keywords and checks title/tags.
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

    /**
     * Parses a query or location configuration object into clean individual tags.
     * Supports Collection (List/Set from multi-select tags), JSON array strings "[a, b]",
     * and comma/semicolon-separated values. Strips quotes and brackets.
     */
    private List<String> parseTags(Object raw) {
        List<String> result = new ArrayList<>();
        if (raw == null) return result;

        if (raw instanceof Collection<?> coll) {
            for (Object item : coll) {
                if (item != null) {
                    String s = cleanTag(item.toString());
                    if (!s.isEmpty()) result.add(s);
                }
            }
            return result;
        }

        String str = raw.toString().trim();
        if (str.startsWith("[") && str.endsWith("]")) {
            str = str.substring(1, str.length() - 1).trim();
        }

        for (String part : str.split("[,;]")) {
            String s = cleanTag(part);
            if (!s.isEmpty()) result.add(s);
        }
        return result;
    }

    private String cleanTag(String s) {
        if (s == null) return "";
        return s.replaceAll("[\\[\\]\"']", "").trim();
    }

    /** Internal record to collect per-provider results. */
    private record ProviderResult(String source, List<JobSearchResult> results, String error) {}
}

