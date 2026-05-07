package com.crescendo.apps.jobsearch;

import java.util.List;
import java.util.Map;

/**
 * Contract for individual job search data sources.
 * Each implementation fetches jobs from one external API/feed and
 * normalises results into {@link JobSearchResult} records.
 */
public interface JobSearchProvider {

    /** Human-readable source name shown in results (e.g. "Google Jobs", "Adzuna"). */
    String sourceName();

    /** Whether this provider needs an API key to function. */
    boolean requiresApiKey();

    /** Returns true if all required keys are present in the step configuration. */
    boolean isEnabled(Map<String, Object> config);

    /**
     * Search for jobs matching the query.
     *
     * @param query    job title / keywords (e.g. "software engineer")
     * @param location location filter (e.g. "India", "Bangalore")
     * @param config   step configuration containing optional API keys
     * @return list of normalized results (may be empty, never null)
     */
    List<JobSearchResult> search(String query, String location, Map<String, Object> config);
}
