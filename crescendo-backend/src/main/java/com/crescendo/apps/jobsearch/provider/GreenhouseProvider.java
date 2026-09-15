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
 * Greenhouse Job Board API provider: public, free, no auth required.
 * <p>
 * Scans company boards on Greenhouse for matching jobs in parallel using Java Virtual Threads.
 * Covers premier Indian product unicorns, global tech giants, AI labs, and quantitative trading firms.
 *
 * @see <a href="https://developers.greenhouse.io/job-board.html">Greenhouse Job Board API docs</a>
 */
public class GreenhouseProvider implements JobSearchProvider {

    private static final Logger log = LoggerFactory.getLogger(GreenhouseProvider.class);
    private static final String API_BASE = "https://boards-api.greenhouse.io/v1/boards/{token}/jobs?content=true";

    private static final Pattern INTERN_PATTERN = Pattern.compile(
            "\\b(intern|internship|co-op|coop|trainee|summer\\s+intern)\\b",
            Pattern.CASE_INSENSITIVE
    );

    /**
     * Curated target companies on Greenhouse across India unicorns, global tech, AI labs, and HFT.
     * Format: boardToken -> companyName
     */
    private static final Map<String, String> DEFAULT_BOARDS = Map.ofEntries(
            // Indian Tech Unicorns & Fintech
            Map.entry("razorpay", "Razorpay"),
            Map.entry("swiggy", "Swiggy"),
            Map.entry("caboratechnology", "Ola"),
            Map.entry("cred", "CRED"),
            Map.entry("groww", "Groww"),
            Map.entry("meesho", "Meesho"),
            Map.entry("dream11", "Dream11"),
            Map.entry("zeta52", "Zeta"),
            Map.entry("postman", "Postman"),
            Map.entry("browserstack", "BrowserStack"),
            Map.entry("chargebee", "Chargebee"),
            Map.entry("freshworks", "Freshworks"),
            Map.entry("clevertap", "CleverTap"),
            Map.entry("inmobi", "InMobi"),
            Map.entry("sharechat", "ShareChat"),
            Map.entry("zepto", "Zepto"),
            Map.entry("blinkit", "Blinkit"),
            Map.entry("atherenergy", "Ather Energy"),
            Map.entry("urbancompany", "Urban Company"),
            Map.entry("slice", "Slice"),
            Map.entry("jupiter", "Jupiter"),
            Map.entry("navi", "Navi"),
            Map.entry("upgrad", "upGrad"),
            Map.entry("licious", "Licious"),
            Map.entry("hasaboratechnologies", "Hasura"),

            // Global Cloud, Infrastructure & Tier-1 Tech
            Map.entry("stripe", "Stripe"),
            Map.entry("datadog", "Datadog"),
            Map.entry("cloudflare", "Cloudflare"),
            Map.entry("figma", "Figma"),
            Map.entry("mongodb", "MongoDB"),
            Map.entry("confluent", "Confluent"),
            Map.entry("gitlab", "GitLab"),
            Map.entry("twilio", "Twilio"),
            Map.entry("hashicorp", "HashiCorp"),
            Map.entry("grafanalabs", "Grafana Labs"),
            Map.entry("elastic", "Elastic"),
            Map.entry("airbnb", "Airbnb"),
            Map.entry("reddit", "Reddit"),

            // Quantitative Trading & HFT
            Map.entry("citadel", "Citadel"),
            Map.entry("twosigma", "Two Sigma"),
            Map.entry("towerresearch", "Tower Research Capital"),
            Map.entry("imctrading", "IMC Trading"),
            Map.entry("millennium", "Millennium"),
            Map.entry("arcesium", "Arcesium"),
            Map.entry("optiver", "Optiver"),

            // Frontier AI Research & Labs
            Map.entry("anthropic", "Anthropic"),
            Map.entry("scaleai", "Scale AI"),
            Map.entry("huggingface", "Hugging Face"),
            Map.entry("cohere", "Cohere"),
            Map.entry("mistral", "Mistral AI")
    );

    @Override public String sourceName() { return "Greenhouse"; }
    @Override public boolean requiresApiKey() { return false; }
    @Override public boolean isEnabled(Map<String, Object> config) { return true; }

    @Override
    public List<JobSearchResult> search(String query, String location, Map<String, Object> config) {
        int limit = parseLimit(config, 50);
        String queryLower = query != null ? query.toLowerCase() : "";
        boolean internshipOnly = isInternshipOnly(config);

        // Allow user to provide custom board tokens as comma-separated string
        Map<String, String> boards = new LinkedHashMap<>(DEFAULT_BOARDS);
        String custom = config != null ? asStr(config.get("greenhouseBoardTokens")) : null;
        if (custom != null && !custom.isBlank()) {
            for (String token : custom.split(",")) {
                String t = token.trim();
                if (!t.isBlank()) boards.put(t, t);
            }
        }

        // Query all company boards in parallel using Java Virtual Threads
        List<CompletableFuture<List<JobSearchResult>>> futures = new ArrayList<>();
        ExecutorService executor = Executors.newVirtualThreadPerTaskExecutor();
        try {
            for (Map.Entry<String, String> entry : boards.entrySet()) {
                String boardToken = entry.getKey();
                String companyName = entry.getValue();

                futures.add(CompletableFuture.supplyAsync(() ->
                        queryBoard(boardToken, companyName, queryLower, location, internshipOnly), executor));
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
    private List<JobSearchResult> queryBoard(String boardToken, String companyName, String queryLower, String location, boolean internshipOnly) {
        try {
            Map<String, Object> response = HTTP
                    .get()
                    .uri(API_BASE, boardToken)
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

                String jobLoc = null;
                if (job.get("location") instanceof Map<?, ?> locMap) {
                    jobLoc = asStr(locMap.get("name"));
                }

                // Location match
                if (!matchesLocation(location, jobLoc)) {
                    continue;
                }

                String desc = asStr(job.get("content"));
                desc = truncate(stripHtml(desc), 500);

                List<String> tags = new ArrayList<>();
                tags.add("Greenhouse");
                if (job.get("departments") instanceof List<?> depts) {
                    for (Object d : depts) {
                        if (d instanceof Map<?, ?> dept && dept.get("name") != null) {
                            tags.add(dept.get("name").toString());
                        }
                    }
                }

                results.add(new JobSearchResult(
                        title,
                        companyName,
                        jobLoc != null ? jobLoc : "Unspecified",
                        asStr(job.get("absolute_url")),
                        null,
                        desc,
                        asStr(job.get("updated_at")),
                        sourceName(),
                        tags,
                        null
                ));
            }
            return results;
        } catch (Exception e) {
            log.debug("[job-search] Greenhouse board '{}' failed: {}", boardToken, e.getMessage());
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
    private String stripHtml(String html) { return html != null ? html.replaceAll("<[^>]*>", " ").replaceAll("\\s+", " ").trim() : null; }
    private int parseLimit(Map<String, Object> config, int def) {
        try { return Integer.parseInt(config.getOrDefault("maxResults", def).toString()); }
        catch (Exception e) { return def; }
    }
}
