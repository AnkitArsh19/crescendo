package com.crescendo.apps.jobsearch;

import com.crescendo.app.App;
import com.crescendo.apps.AppDefinition;
import com.crescendo.enums.AuthType;
import org.springframework.stereotype.Component;

import java.util.List;
import java.util.Map;

/**
 * Job Search app definition for the Crescendo catalog.
 * <p>
 * Aggregates jobs from 10+ sources focused on the Indian job market:
 * <ul>
 *   <li><b>Free (no auth):</b> LinkedIn public, Greenhouse boards (Razorpay, Swiggy,
 *       CRED, etc.), Lever boards (Atlan, MoEngage), Remotive, Arbeitnow, Himalayas</li>
 *   <li><b>Platform-managed keys:</b> SerpAPI (Google Jobs), Adzuna India, Jooble —
 *       keys are configured in application.properties, not by users.</li>
 * </ul>
 */
@Component
public class JobSearchApp implements AppDefinition {

    @Override
    public App toApp() {
        return new App("job-search", "Job Search",
                "Search jobs across 10+ platforms — India focused",
                "/icons/job-search.svg", AuthType.NONE,
                List.of(), // no triggers
                List.of(
                    Map.of(
                        "actionKey", "search-jobs",
                        "name", "Search Jobs (India)",
                        "description", "Search across LinkedIn, Greenhouse (Razorpay/Swiggy/CRED/…), Lever, Google Jobs, Adzuna, Jooble, Remotive, and more",
                        "configSchema", List.of(
                            Map.of("key", "query", "label", "Job Title / Keywords",
                                   "type", "multi_select_tags", "required", true,
                                   "placeholder", "Type a role or pick from suggestions",
                                   "helpText", "Select or type one or more job titles. Each keyword is searched across all sources.",
                                   "options", List.of(
                                       "Software Engineer", "Frontend Developer", "Backend Developer",
                                       "Full Stack Developer", "Data Scientist", "Data Analyst",
                                       "ML Engineer", "DevOps Engineer", "Product Manager",
                                       "QA Engineer", "Mobile Developer", "Cloud Engineer",
                                       "UI/UX Designer", "System Administrator", "Intern"
                                   )),
                            Map.of("key", "location", "label", "Location",
                                   "type", "multi_select_tags", "required", false,
                                   "placeholder", "Type a city or pick from suggestions",
                                   "helpText", "Select or type locations (default: India)",
                                   "options", List.of(
                                       "Bangalore", "Mumbai", "Delhi", "Hyderabad", "Chennai",
                                       "Pune", "Noida", "Gurgaon", "Kolkata", "Ahmedabad",
                                       "Remote", "India", "USA", "UK", "Germany", "Canada"
                                   )),
                            Map.of("key", "maxResults", "label", "Max Total Results",
                                   "type", "number", "required", false,
                                   "helpText", "Maximum total results after deduplication (default: 50, max: 100)"),

                            // -- LinkedIn filters (build URL automatically) --
                            Map.of("key", "linkedInExperience", "label", "Experience Level",
                                   "type", "select", "required", false,
                                   "options", List.of(
                                       Map.of("value", "", "label", "Any"),
                                       Map.of("value", "1", "label", "Internship"),
                                       Map.of("value", "2", "label", "Entry Level"),
                                       Map.of("value", "3", "label", "Associate"),
                                       Map.of("value", "4", "label", "Mid-Senior"),
                                       Map.of("value", "5", "label", "Director"),
                                       Map.of("value", "6", "label", "Executive")
                                   ),
                                   "helpText", "Filter by experience level (applied across all sources)"),
                            Map.of("key", "linkedInJobType", "label", "Job Type",
                                   "type", "select", "required", false,
                                   "options", List.of(
                                       Map.of("value", "", "label", "Any"),
                                       Map.of("value", "F", "label", "Full-time"),
                                       Map.of("value", "P", "label", "Part-time"),
                                       Map.of("value", "C", "label", "Contract"),
                                       Map.of("value", "T", "label", "Temporary"),
                                       Map.of("value", "I", "label", "Internship")
                                   ),
                                   "helpText", "Filter by employment type (applied across all sources)"),
                            Map.of("key", "linkedInWorkType", "label", "Work Type",
                                   "type", "select", "required", false,
                                   "options", List.of(
                                       Map.of("value", "", "label", "Any"),
                                       Map.of("value", "1", "label", "On-site"),
                                       Map.of("value", "2", "label", "Remote"),
                                       Map.of("value", "3", "label", "Hybrid")
                                   ),
                                   "helpText", "Filter by work arrangement (applied across all sources)"),
                            Map.of("key", "linkedInDatePosted", "label", "Date Posted",
                                   "type", "select", "required", false,
                                   "options", List.of(
                                       Map.of("value", "", "label", "Any time"),
                                       Map.of("value", "r86400", "label", "Past 24 hours"),
                                       Map.of("value", "r604800", "label", "Past week"),
                                       Map.of("value", "r2592000", "label", "Past month")
                                   ),
                                   "helpText", "Filter by posting recency (used by LinkedIn, SerpAPI)"),

                            // -- LinkedIn raw URL override (advanced) --
                            Map.of("key", "linkedInSearchUrl", "label", "LinkedIn Search URL (advanced, optional)",
                                   "type", "text", "required", false,
                                   "placeholder", "https://www.linkedin.com/jobs/search/?keywords=...&f_TPR=r604800",
                                   "helpText", "Advanced: paste a full LinkedIn search URL to override the filters above. All URL params will be forwarded."),

                            // -- ATS board customization --
                            Map.of("key", "greenhouseBoardTokens", "label", "Extra Greenhouse Boards",
                                   "type", "text", "required", false,
                                   "placeholder", "e.g. flipkart,phonepe,freshworks",
                                   "helpText", "Comma-separated Greenhouse board tokens to scan in addition to the 15 built-in Indian companies (Razorpay, Swiggy, CRED, Groww, etc.)"),
                            Map.of("key", "leverBoardSlugs", "label", "Extra Lever Boards",
                                   "type", "text", "required", false,
                                   "placeholder", "e.g. zerodha,slice,coinswitch",
                                   "helpText", "Comma-separated Lever company slugs to scan in addition to the built-in list")
                        )
                    ),
                    Map.of(
                        "actionKey", "search-remote-jobs",
                        "name", "Search Remote Jobs",
                        "description", "Search remote-friendly jobs from Remotive, Arbeitnow, and Himalayas (no API key needed)",
                        "configSchema", List.of(
                            Map.of("key", "query", "label", "Job Title / Keywords",
                                   "type", "multi_select_tags", "required", true,
                                   "placeholder", "Type a role or pick from suggestions",
                                   "helpText", "Select or type the job title or keywords to search for",
                                   "options", List.of(
                                       "Software Engineer", "Frontend Developer", "Backend Developer",
                                       "Full Stack Developer", "Data Scientist", "DevOps Engineer",
                                       "Product Manager", "ML Engineer", "Designer", "Intern"
                                   )),
                            Map.of("key", "maxResults", "label", "Max Results Per Source",
                                   "type", "number", "required", false,
                                   "helpText", "Maximum results per source (default: 10)")
                        )
                    )
                ))
                .category("productivity")
                .helpUrl("https://serpapi.com/google-jobs-api");
    }
}
