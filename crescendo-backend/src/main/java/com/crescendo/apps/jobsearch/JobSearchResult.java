package com.crescendo.apps.jobsearch;

import java.util.List;

/**
 * Normalized job search result, agnostic of the source API.
 * All providers map their response to this schema so results can be
 * merged, deduplicated, and sorted uniformly.
 */
public record JobSearchResult(
        String title,
        String company,
        String location,
        String url,
        String salary,
        String description,
        String postedDate,
        String source,
        List<String> tags,
        String jobType
) {
    /**
     * Deduplication key: lowercase company + title.
     * Two results with the same key from different sources are considered duplicates.
     */
    public String deduplicationKey() {
        String c = (company != null ? company : "").toLowerCase().trim();
        String t = (title != null ? title : "").toLowerCase().trim();
        return c + "|" + t;
    }
}
