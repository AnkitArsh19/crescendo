package com.crescendo.apps.jobsearch.provider;

import com.crescendo.apps.jobsearch.JobSearchProvider;
import com.crescendo.apps.jobsearch.JobSearchResult;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.http.HttpHeaders;
import org.springframework.web.client.RestClient;

import java.net.URI;
import java.net.URLDecoder;
import java.nio.charset.StandardCharsets;
import java.util.*;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

/**
 * LinkedIn public guest jobs endpoint — works without authentication.
 * <p>
 * Supports three modes (in priority order):
 * <ol>
 *   <li><b>LinkedIn Search URL</b>: User pastes a full linkedin.com/jobs/search URL
 *       with all desired filters. All params are forwarded to the guest API.</li>
 *   <li><b>Filter fields</b>: User selects experience level, job type, work type,
 *       date posted from dropdown menus. The provider builds the LinkedIn URL
 *       automatically from these selections.</li>
 *   <li><b>Keywords + Location</b>: Fallback — simple search with query and location.</li>
 * </ol>
 *
 * <p>LinkedIn URL filter params include:
 * <ul>
 *   <li>{@code f_TPR} — Time posted (r86400=24h, r604800=7d, r2592000=30d)</li>
 *   <li>{@code f_E} — Experience level (1=Intern, 2=Entry, 3=Associate, 4=Mid-Senior, 5=Director, 6=Executive)</li>
 *   <li>{@code f_JT} — Job type (F=Full-time, P=Part-time, C=Contract, T=Temporary, I=Internship)</li>
 *   <li>{@code f_WT} — Workplace type (1=On-site, 2=Remote, 3=Hybrid)</li>
 *   <li>{@code f_SB2} — Salary range</li>
 *   <li>{@code f_AL} — Easy Apply filter (true)</li>
 *   <li>{@code sortBy} — DD = sort by date</li>
 * </ul>
 *
 * @see <a href="https://www.linkedin.com/jobs/search/">LinkedIn Jobs Search</a>
 */
public class LinkedInGuestProvider implements JobSearchProvider {

    private static final Logger log = LoggerFactory.getLogger(LinkedInGuestProvider.class);
    private static final String GUEST_API = "https://www.linkedin.com/jobs-guest/jobs/api/seeMoreJobPostings/search";

    // Rotate User-Agent to reduce rate-limit risk
    private static final String[] USER_AGENTS = {
            "Mozilla/5.0 (Macintosh; Intel Mac OS X 10_15_7) AppleWebKit/537.36 (KHTML, like Gecko) Chrome/120.0.0.0 Safari/537.36",
            "Mozilla/5.0 (Windows NT 10.0; Win64; x64) AppleWebKit/537.36 (KHTML, like Gecko) Chrome/121.0.0.0 Safari/537.36",
            "Mozilla/5.0 (X11; Linux x86_64) AppleWebKit/537.36 (KHTML, like Gecko) Chrome/119.0.0.0 Safari/537.36",
            "Mozilla/5.0 (Macintosh; Intel Mac OS X 10_15_7) AppleWebKit/605.1.15 (KHTML, like Gecko) Version/17.2 Safari/605.1.15",
            "Mozilla/5.0 (Windows NT 10.0; Win64; x64; rv:121.0) Gecko/20100101 Firefox/121.0",
    };
    private static final Random RNG = new Random();

    @Override public String sourceName() { return "LinkedIn"; }
    @Override public boolean requiresApiKey() { return false; }
    @Override public boolean isEnabled(Map<String, Object> config) { return true; }

    @Override
    public List<JobSearchResult> search(String query, String location, Map<String, Object> config) {
        int limit = parseLimit(config, 10);

        // Priority 1: User provided a full LinkedIn search URL
        String linkedInUrl = config != null ? asStr(config.get("linkedInSearchUrl")) : null;

        String requestUrl;
        if (linkedInUrl != null && !linkedInUrl.isBlank() && linkedInUrl.contains("linkedin.com/jobs")) {
            requestUrl = buildUrlFromLinkedInSearch(linkedInUrl);
        } else {
            // Priority 2 & 3: Build URL from filter fields (or fallback to keywords+location)
            requestUrl = buildUrlFromFilters(query, location, config);
        }

        // Add small random delay (500-2000ms) to reduce rate-limit risk
        try {
            Thread.sleep(500 + RNG.nextInt(1500));
        } catch (InterruptedException e) {
            Thread.currentThread().interrupt();
        }

        try {
            String html = RestClient.create()
                    .get()
                    .uri(requestUrl)
                    .header(HttpHeaders.USER_AGENT, USER_AGENTS[RNG.nextInt(USER_AGENTS.length)])
                    .header(HttpHeaders.ACCEPT, "text/html")
                    .header("Accept-Language", "en-US,en;q=0.9")
                    .retrieve()
                    .body(String.class);

            if (html == null || html.isBlank()) return List.of();

            return parseJobCards(html, limit);
        } catch (Exception e) {
            log.warn("[job-search] LinkedIn guest API failed: {}", e.getMessage());
            return List.of();
        }
    }

    /**
     * Builds the guest API URL from user-friendly filter fields in the config.
     * <p>
     * Reads: linkedInExperience, linkedInJobType, linkedInWorkType, linkedInDatePosted
     * and constructs the appropriate LinkedIn filter params (f_E, f_JT, f_WT, f_TPR).
     */
    private String buildUrlFromFilters(String query, String location, Map<String, Object> config) {
        String loc = (location != null && !location.isBlank()) ? location : "India";
        StringBuilder sb = new StringBuilder(GUEST_API)
                .append("?keywords=").append(encode(query))
                .append("&location=").append(encode(loc))
                .append("&sortBy=DD"); // Always sort by date (newest first)

        if (config != null) {
            // Experience level: f_E=1,2,3,4,5,6
            String exp = asStr(config.get("linkedInExperience"));
            if (exp != null && !exp.isBlank()) {
                sb.append("&f_E=").append(exp);
            }

            // Job type: f_JT=F,P,C,T,I
            String jobType = asStr(config.get("linkedInJobType"));
            if (jobType != null && !jobType.isBlank()) {
                sb.append("&f_JT=").append(jobType);
            }

            // Work type: f_WT=1,2,3
            String workType = asStr(config.get("linkedInWorkType"));
            if (workType != null && !workType.isBlank()) {
                sb.append("&f_WT=").append(workType);
            }

            // Date posted: f_TPR=r86400, r604800, r2592000
            String datePosted = asStr(config.get("linkedInDatePosted"));
            if (datePosted != null && !datePosted.isBlank()) {
                sb.append("&f_TPR=").append(datePosted);
            }
        }

        sb.append("&start=0");

        String url = sb.toString();
        log.info("[job-search] LinkedIn filter mode — {}", summarizeBuiltUrl(config, query, loc));
        return url;
    }

    /** Logs a human-readable summary of the filters applied. */
    private String summarizeBuiltUrl(Map<String, Object> config, String query, String location) {
        StringBuilder s = new StringBuilder("keywords=").append(query).append(", location=").append(location);
        if (config != null) {
            String exp = asStr(config.get("linkedInExperience"));
            if (exp != null && !exp.isBlank()) s.append(", experience=").append(exp);
            String jt = asStr(config.get("linkedInJobType"));
            if (jt != null && !jt.isBlank()) s.append(", jobType=").append(jt);
            String wt = asStr(config.get("linkedInWorkType"));
            if (wt != null && !wt.isBlank()) s.append(", workType=").append(wt);
            String dp = asStr(config.get("linkedInDatePosted"));
            if (dp != null && !dp.isBlank()) s.append(", datePosted=").append(dp);
        }
        return s.toString();
    }

    /**
     * Converts a LinkedIn jobs search page URL into the equivalent guest API URL.
     * <p>
     * Input:  {@code https://www.linkedin.com/jobs/search/?keywords=Java&location=India&f_TPR=r604800&f_E=3,4}
     * Output: {@code https://www.linkedin.com/jobs-guest/jobs/api/seeMoreJobPostings/search?keywords=Java&location=India&f_TPR=r604800&f_E=3,4&start=0}
     */
    private String buildUrlFromLinkedInSearch(String linkedInUrl) {
        try {
            URI uri = URI.create(linkedInUrl.trim());
            String queryString = uri.getRawQuery();

            if (queryString == null || queryString.isBlank()) {
                return GUEST_API + "?start=0";
            }

            StringBuilder sb = new StringBuilder(GUEST_API).append("?");
            sb.append(queryString);

            if (!queryString.contains("start=")) {
                sb.append("&start=0");
            }

            String result = sb.toString();
            log.info("[job-search] LinkedIn URL mode — using filters from user URL: {}", summarizeParams(queryString));
            return result;

        } catch (Exception e) {
            log.warn("[job-search] Failed to parse LinkedIn URL '{}': {}", linkedInUrl, e.getMessage());
            return GUEST_API + "?start=0";
        }
    }

    /** Logs a human-readable summary of the LinkedIn filter params. */
    private String summarizeParams(String queryString) {
        Map<String, String> filterNames = Map.of(
                "f_TPR", "Time Posted",
                "f_E", "Experience",
                "f_JT", "Job Type",
                "f_WT", "Workplace",
                "f_AL", "Easy Apply",
                "f_SB2", "Salary",
                "keywords", "Keywords",
                "location", "Location"
        );

        StringBuilder summary = new StringBuilder();
        for (String param : queryString.split("&")) {
            String[] kv = param.split("=", 2);
            if (kv.length == 2) {
                String name = filterNames.getOrDefault(kv[0], kv[0]);
                String value = URLDecoder.decode(kv[1], StandardCharsets.UTF_8);
                if (!summary.isEmpty()) summary.append(", ");
                summary.append(name).append("=").append(value);
            }
        }
        return summary.toString();
    }

    /**
     * Parses LinkedIn job cards from HTML response.
     * Each job card is inside a {@code <li>} with base-card elements.
     */
    private List<JobSearchResult> parseJobCards(String html, int limit) {
        List<JobSearchResult> results = new ArrayList<>();

        Pattern liPattern = Pattern.compile("<li[^>]*>([\\s\\S]*?)</li>", Pattern.CASE_INSENSITIVE);
        Matcher liMatcher = liPattern.matcher(html);

        while (liMatcher.find() && results.size() < limit) {
            String card = liMatcher.group(0);

            if (!card.contains("base-card") && !card.contains("job-search-card")) continue;

            String title = extractText(card, "base-search-card__title");
            String company = extractText(card, "base-search-card__subtitle");
            String jobLocation = extractText(card, "job-search-card__location");
            String url = extractHref(card, "base-card__full-link");
            String postedDate = extractDatetime(card);

            if (title != null && !title.isBlank()) {
                results.add(new JobSearchResult(
                        title.trim(),
                        company != null ? company.trim() : null,
                        jobLocation != null ? jobLocation.trim() : null,
                        url,
                        null,
                        null,
                        postedDate,
                        sourceName(),
                        List.of("LinkedIn"),
                        null
                ));
            }
        }

        return results;
    }

    private String extractText(String html, String className) {
        Pattern p = Pattern.compile(
                "class=\"[^\"]*" + Pattern.quote(className) + "[^\"]*\"[^>]*>([^<]*)<",
                Pattern.CASE_INSENSITIVE);
        Matcher m = p.matcher(html);
        return m.find() ? m.group(1).trim() : null;
    }

    private String extractHref(String html, String className) {
        Pattern p = Pattern.compile(
                "<a[^>]*class=\"[^\"]*" + Pattern.quote(className) + "[^\"]*\"[^>]*href=\"([^\"]+)\"",
                Pattern.CASE_INSENSITIVE);
        Matcher m = p.matcher(html);
        if (!m.find()) {
            p = Pattern.compile(
                    "<a[^>]*href=\"([^\"]+)\"[^>]*class=\"[^\"]*" + Pattern.quote(className) + "[^\"]*\"",
                    Pattern.CASE_INSENSITIVE);
            m = p.matcher(html);
        }
        if (m.find()) {
            String href = m.group(1);
            int q = href.indexOf('?');
            return q > 0 ? href.substring(0, q) : href;
        }
        return null;
    }

    private String extractDatetime(String html) {
        Pattern p = Pattern.compile("datetime=\"([^\"]+)\"");
        Matcher m = p.matcher(html);
        return m.find() ? m.group(1) : null;
    }

    private String encode(String s) {
        return java.net.URLEncoder.encode(s, StandardCharsets.UTF_8);
    }

    private String asStr(Object v) { return v != null ? v.toString() : null; }

    private int parseLimit(Map<String, Object> config, int def) {
        try { return Integer.parseInt(config.getOrDefault("maxResults", def).toString()); }
        catch (Exception e) { return def; }
    }
}
