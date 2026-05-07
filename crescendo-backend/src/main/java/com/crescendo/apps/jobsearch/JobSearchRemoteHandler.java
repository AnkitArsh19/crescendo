package com.crescendo.apps.jobsearch;

import com.crescendo.apps.jobsearch.provider.*;
import com.crescendo.execution.action.ActionContext;
import com.crescendo.execution.action.ActionHandler;
import com.crescendo.execution.action.ActionMapping;
import com.crescendo.execution.action.ActionResult;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.util.*;
import java.util.concurrent.*;

/**
 * Searches remote-only job sources (Remotive, Arbeitnow, Himalayas).
 * No API keys required — fully free.
 */
@ActionMapping(appKey = "job-search", actionKey = "search-remote-jobs")
public class JobSearchRemoteHandler implements ActionHandler {

    private static final Logger log = LoggerFactory.getLogger(JobSearchRemoteHandler.class);
    private static final int PROVIDER_TIMEOUT_SECONDS = 5;

    private final List<JobSearchProvider> providers = List.of(
            new RemotiveProvider(),
            new ArbeitnowProvider(),
            new HimalayasProvider()
    );

    @Override
    public ActionResult execute(ActionContext context) {
        Map<String, Object> config = context.configuration();

        String rawQuery = config.get("query") != null ? config.get("query").toString().trim() : null;
        if (rawQuery == null || rawQuery.isBlank()) {
            return ActionResult.failure("'query' is required — enter a job title or keywords");
        }

        // Support comma-separated roles
        List<String> queries = new ArrayList<>();
        for (String kw : rawQuery.split(",")) {
            String trimmed = kw.trim();
            if (!trimmed.isEmpty()) queries.add(trimmed);
        }
        if (queries.isEmpty()) {
            return ActionResult.failure("'query' is required — enter a job title or keywords");
        }

        log.info("[job-search] Remote-only search for {} keyword(s): [{}] across {} sources",
                queries.size(), String.join(", ", queries), providers.size());

        ExecutorService executor = Executors.newVirtualThreadPerTaskExecutor();
        List<CompletableFuture<ProviderResult>> futures = new ArrayList<>();

        for (String query : queries) {
            for (JobSearchProvider provider : providers) {
                futures.add(CompletableFuture.supplyAsync(() -> {
                    try {
                        List<JobSearchResult> results = provider.search(query, "Remote", config);
                        return new ProviderResult(provider.sourceName(), results, null);
                    } catch (Exception e) {
                        return new ProviderResult(provider.sourceName(), List.of(), e.getMessage());
                    }
                }, executor).orTimeout(PROVIDER_TIMEOUT_SECONDS, TimeUnit.SECONDS)
                 .exceptionally(ex -> new ProviderResult("unknown", List.of(), "Timed out")));
            }
        }

        List<ProviderResult> allResults = futures.stream().map(CompletableFuture::join).toList();
        executor.shutdown();

        LinkedHashMap<String, JobSearchResult> deduplicated = new LinkedHashMap<>();
        List<Map<String, Object>> sourceStats = new ArrayList<>();

        for (ProviderResult pr : allResults) {
            sourceStats.add(Map.of("source", pr.source, "count", pr.results.size()));
            for (JobSearchResult result : pr.results) {
                deduplicated.putIfAbsent(result.deduplicationKey(), result);
            }
        }

        int maxResults = 25;
        try { maxResults = Math.min(Integer.parseInt(config.getOrDefault("maxResults", 25).toString()), 100); }
        catch (Exception ignored) {}

        List<Map<String, Object>> jobList = deduplicated.values().stream()
                .sorted(Comparator.comparing(r -> r.title() != null ? r.title().toLowerCase() : ""))
                .limit(maxResults)
                .map(this::toMap)
                .toList();

        Map<String, Object> output = new LinkedHashMap<>();
        output.put("jobs", jobList);
        output.put("totalFound", jobList.size());
        output.put("sources", sourceStats);
        output.put("query", rawQuery);
        output.put("keywords", queries);

        log.info("[job-search] Remote jobs fetched successfully");
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
        return m;
    }

    private record ProviderResult(String source, List<JobSearchResult> results, String error) {}
}
